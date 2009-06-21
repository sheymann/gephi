/*
Copyright 2008 WebAtlas
Authors : Mathieu Bastian, Mathieu Jacomy, Julian Bilcke
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gephi.visualization.opengl.compatibility;

import com.sun.opengl.util.BufferUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import org.gephi.visualization.VizController;
import org.gephi.visualization.api.objects.ModelClass;
import org.gephi.visualization.opengl.AbstractEngine;
import org.gephi.visualization.api.ModelImpl;
import org.gephi.visualization.api.initializer.CompatibilityModeler;
import org.gephi.visualization.opengl.octree.Octree;
import org.gephi.visualization.api.Scheduler;
import org.gephi.visualization.api.VizConfig.DisplayConfig;
import org.gephi.visualization.api.objects.CompatibilityModelClass;
import org.gephi.visualization.opengl.compatibility.objects.Potato3dModel;

/**
 *
 * @author Mathieu Bastian
 */
public class CompatibilityEngine extends AbstractEngine {

    Octree octree;
    private CompatibilityScheduler scheduler;

    //User config
    protected CompatibilityModelClass[] modelClasses;
    protected CompatibilityModelClass[] lodClasses;
    protected CompatibilityModelClass[] selectableClasses;
    protected CompatibilityModelClass[] clickableClasses;

    //Selection
    private ConcurrentLinkedQueue<ModelImpl>[] selectedObjects;

    public CompatibilityEngine() {
        super();
    }

    @Override
    public void initArchitecture() {
        super.initArchitecture();
        scheduler = (CompatibilityScheduler) VizController.getInstance().getScheduler();
        vizEventManager = VizController.getInstance().getVizEventManager();

        //Init
        octree = new Octree(vizConfig.getOctreeDepth(), vizConfig.getOctreeWidth(), modelClasses.length);
        octree.initArchitecture();
    }

    public void updateSelection(GL gl, GLU glu) {
        octree.updateSelectedOctant(gl, glu, graphIO.getMousePosition(), currentSelectionArea.getSelectionAreaRectancle());

        //Potatoes selection
        if (modelClasses[CLASS_POTATO].isEnabled()) {

            int potatoCount = octree.countSelectedObjects(CLASS_POTATO);
            float[] mousePosition = graphIO.getMousePosition();
            float[] pickRectangle = currentSelectionArea.getSelectionAreaRectancle();

            //Update selection
            int capacity = 1 * 4 * potatoCount;      //Each object take in maximium : 4 * name stack depth
            IntBuffer hitsBuffer = BufferUtil.newIntBuffer(capacity);

            gl.glSelectBuffer(hitsBuffer.capacity(), hitsBuffer);
            gl.glRenderMode(GL.GL_SELECT);

            gl.glInitNames();
            gl.glPushName(0);

            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();

            glu.gluPickMatrix(mousePosition[0], mousePosition[1], pickRectangle[0], pickRectangle[1], graphDrawable.getViewport());
            gl.glMultMatrixd(graphDrawable.getProjectionMatrix());

            gl.glMatrixMode(GL.GL_MODELVIEW);

            //Draw the nodes' cube int the select buffer
            int hitName = 1;
            ModelImpl[] array = new ModelImpl[potatoCount];
            for (Iterator<ModelImpl> itr = octree.getSelectedObjectIterator(CLASS_POTATO); itr.hasNext();) {
                Potato3dModel obj = (Potato3dModel) itr.next();
                obj.setUnderMouse(false);
                if (obj.isDisplayReady()) {
                    array[hitName - 1] = obj;
                    gl.glLoadName(hitName);
                    obj.mark = false;
                    gl.glBegin(GL.GL_TRIANGLES);
                    obj.display(gl, glu);
                    gl.glEnd();
                    obj.mark = true;
                    obj.display(gl, glu);
                    obj.mark = false;
                    hitName++;
                }
            }

            //Restoring the original projection matrix
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPopMatrix();
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glFlush();

            //Returning to normal rendering mode
            int nbRecords = gl.glRenderMode(GL.GL_RENDER);

            //Get the hits and put the node under selection in the selectionArray
            for (int i = 0; i < nbRecords; i++) {
                int hit = hitsBuffer.get(i * 4 + 3) - 1; 		//-1 Because of the glPushName(0)
                Potato3dModel obj = (Potato3dModel) array[hit];
                if (!obj.isParentUnderMouse()) {
                    obj.setUnderMouse(true);
                }
            }
        }
    }

    @Override
    public boolean updateWorld() {
        if (dataBridge.requireUpdate()) {
            dataBridge.updateWorld();
            return true;
        }
        return false;
    }

    @Override
    public void worldUpdated(int cacheMarker) {
        octree.setCacheMarker(cacheMarker);
        for (ModelClass objClass : modelClasses) {
            if (objClass.getCacheMarker() == cacheMarker) {
                octree.cleanDeletedObjects(objClass.getClassId());
            }
        }
    }

    @Override
    public void beforeDisplay(GL gl, GLU glu) {
    }

    @Override
    public void display(GL gl, GLU glu) {

        for (Iterator<ModelImpl> itr = octree.getObjectIterator(CLASS_NODE); itr.hasNext();) {
            ModelImpl obj = itr.next();
            modelClasses[CLASS_NODE].getCurrentModeler().chooseModel(obj);
            setViewportPosition(obj);
        }

        long startTime = System.currentTimeMillis();

        if (modelClasses[CLASS_EDGE].isEnabled()) {
            gl.glDisable(GL.GL_LIGHTING);
            //gl.glLineWidth(obj.getObj().getSize());
            //gl.glDisable(GL.GL_BLEND);
            //gl.glBegin(GL.GL_LINES);
            //gl.glBegin(GL.GL_QUADS);
            gl.glBegin(GL.GL_TRIANGLES);

            if (vizConfig.getDisplayConfig() == DisplayConfig.DISPLAY_ALL) {
                //Normal mode, all edges rendered
                for (Iterator<ModelImpl> itr = octree.getObjectIterator(CLASS_EDGE); itr.hasNext();) {
                    ModelImpl obj = itr.next();
                    //Renderable renderable = obj.getObj();

                    if (obj.markTime != startTime) {
                        obj.display(gl, glu);
                        obj.markTime = startTime;
                    }

                }
            } else if (vizConfig.getDisplayConfig() == DisplayConfig.DISPLAY_NODES_EDGES) {
                //Only edges on selected nodes are rendered
                for (Iterator<ModelImpl> itr = octree.getSelectedObjectIterator(CLASS_EDGE); itr.hasNext();) {
                    ModelImpl obj = itr.next();
                    if (obj.isSelected() && obj.markTime != startTime) {
                        obj.display(gl, glu);
                        obj.markTime = startTime;
                    }
                }
            } else if (vizConfig.getDisplayConfig() == DisplayConfig.DISPLAY_ALPHA) {
                //Selected edges are rendered with 1f alpha, half otherwise
                for (Iterator<ModelImpl> itr = octree.getObjectIterator(CLASS_EDGE); itr.hasNext();) {
                    ModelImpl obj = itr.next();
                    if (obj.markTime != startTime) {
                        obj.getObj().setAlpha(obj.isSelected() ? 1f : 0.2f);
                        obj.display(gl, glu);
                        obj.markTime = startTime;
                    }
                }
            }
            gl.glEnd();
            gl.glEnable(GL.GL_LIGHTING);
        //gl.glEnable(GL.GL_BLEND);
        }

        //Node
        if (modelClasses[CLASS_NODE].isEnabled()) {
            if (vizConfig.getDisplayConfig() == DisplayConfig.DISPLAY_ALPHA) {
                //Selected nodes are rendered with 1f alpha, half otherwise
                for (Iterator<ModelImpl> itr = octree.getObjectIterator(CLASS_NODE); itr.hasNext();) {
                    ModelImpl obj = itr.next();
                    if (obj.markTime != startTime) {
                        obj.getObj().setAlpha(obj.isSelected() ? 1f : 0.4f);
                        obj.display(gl, glu);
                        obj.markTime = startTime;
                    }
                }
            } else {
                //Mode normal
                for (Iterator<ModelImpl> itr = octree.getObjectIterator(CLASS_NODE); itr.hasNext();) {
                    ModelImpl obj = itr.next();
                    if (obj.markTime != startTime) {
                        obj.display(gl, glu);
                        obj.markTime = startTime;
                    }
                }
            }
        }

        //Arrows
        if (modelClasses[CLASS_ARROW].isEnabled()) {
            gl.glBegin(GL.GL_TRIANGLES);
            for (Iterator<ModelImpl> itr = octree.getObjectIterator(CLASS_ARROW); itr.hasNext();) {
                ModelImpl obj = itr.next();
                if (obj.markTime != startTime) {
                    obj.display(gl, glu);
                    obj.markTime = startTime;
                }
            }
            gl.glEnd();
        }

        //Potatoes
        if (modelClasses[CLASS_POTATO].isEnabled()) {
            //gl.glDisable(GL.GL_LIGHTING);

            //Triangles
            gl.glDisable(GL.GL_LIGHTING);
            gl.glBegin(GL.GL_TRIANGLES);
            for (Iterator<ModelImpl> itr = octree.getObjectIterator(CLASS_POTATO); itr.hasNext();) {
                ModelImpl obj = itr.next();
                if (!obj.mark) {
                    obj.display(gl, glu);
                    obj.mark = true;
                }
            }
            gl.glEnd();
            gl.glEnable(GL.GL_LIGHTING);

            //Solid disk
            for (Iterator<ModelImpl> itr = octree.getObjectIterator(CLASS_POTATO); itr.hasNext();) {
                ModelImpl obj = itr.next();
                if (obj.markTime != startTime) {
                    obj.display(gl, glu);
                    obj.markTime = startTime;
                    obj.mark = false;
                }
            }
        //gl.glEnable(GL.GL_LIGHTING);
        }

        octree.displayOctree(gl);
    }

    @Override
    public void afterDisplay(GL gl, GLU glu) {
    }

    @Override
    public void cameraHasBeenMoved(GL gl, GLU glu) {
    }

    @Override
    public void initEngine(final GL gl, final GLU glu) {
        initDisplayLists(gl, glu);
        scheduler.cameraMoved.set(true);
        scheduler.mouseMoved.set(true);
        lifeCycle.setInited();
    }

    @Override
    public void addObject(int classID, ModelImpl obj) {
        octree.addObject(classID, obj);
    }

    @Override
    public void removeObject(int classID, ModelImpl obj) {
        octree.removeObject(classID, obj);
    }

    @Override
    public void resetObjectClass(ModelClass object3dClass) {
        octree.resetObjectClass(object3dClass.getClassId());
    }

    @Override
    public void mouseClick() {
        for (ModelClass objClass : clickableClasses) {
            ModelImpl[] objArray = selectedObjects[objClass.getSelectionId()].toArray(new ModelImpl[0]);
            if (objArray.length > 0) {
                eventBridge.mouseClick(objClass, objArray);
            }
        }
    }

    @Override
    public void mouseDrag() {
        float[] drag = graphIO.getMouseDrag();
        for (ModelImpl obj : selectedObjects[0]) {
            float[] mouseDistance = obj.getDragDistanceFromMouse();
            obj.getObj().setX(drag[0] + mouseDistance[0]);
            obj.getObj().setY(drag[1] + mouseDistance[1]);
        }
    }

    @Override
    public void mouseMove() {

        List<ModelImpl> newSelectedObjects = null;
        List<ModelImpl> unSelectedObjects = null;

        if (vizEventManager.hasSelectionListeners()) {
            newSelectedObjects = new ArrayList<ModelImpl>();
            unSelectedObjects = new ArrayList<ModelImpl>();
        }

        long markTime = System.currentTimeMillis();
        int i = 0;
        for (ModelClass objClass : selectableClasses) {
            for (Iterator<ModelImpl> itr = octree.getSelectedObjectIterator(objClass.getClassId()); itr.hasNext();) {
                ModelImpl obj = itr.next();
                if (isUnderMouse(obj) && currentSelectionArea.select(obj.getObj())) {
                    if (!obj.isSelected()) {
                        //New selected
                        obj.setSelected(true);
                        if (vizEventManager.hasSelectionListeners()) {
                            newSelectedObjects.add(obj);
                        }
                        selectedObjects[i].add(obj);
                    }
                    obj.selectionMark = markTime;
                } else if (currentSelectionArea.unselect(obj.getObj())) {
                    if (vizEventManager.hasSelectionListeners() && obj.isSelected()) {
                        unSelectedObjects.add(obj);
                    }
                }
            }

            for (Iterator<ModelImpl> itr = selectedObjects[i].iterator(); itr.hasNext();) {
                ModelImpl o = itr.next();
                if (o.selectionMark != markTime) {
                    itr.remove();
                    o.setSelected(false);
                }
            }
            i++;
        }
    }

    @Override
    public void refreshGraphLimits() {
    }

    @Override
    public void startDrag() {
        float x = graphIO.getMouseDrag()[0];
        float y = graphIO.getMouseDrag()[1];

        for (Iterator<ModelImpl> itr = selectedObjects[0].iterator(); itr.hasNext();) {
            ModelImpl o = itr.next();
            float[] tab = o.getDragDistanceFromMouse();
            tab[0] = o.getObj().x() - x;
            tab[1] = o.getObj().y() - y;
        }
    }

    @Override
    public void stopDrag() {
        scheduler.requireUpdatePosition();
    }

    @Override
    public void updateObjectsPosition() {
        for (ModelClass objClass : modelClasses) {
            if (objClass.isEnabled()) {
                octree.updateObjectsPosition(objClass.getClassId());
            }
        }
    }

    private void initDisplayLists(GL gl, GLU glu) {
        //Constants
        float blancCasse[] = {(float) 213 / 255, (float) 208 / 255, (float) 188 / 255, 1.0f};
        float noirCasse[] = {(float) 39 / 255, (float) 25 / 255, (float) 99 / 255, 1.0f};
        float noir[] = {(float) 0 / 255, (float) 0 / 255, (float) 0 / 255, 0.0f};
        float[] shine_low = {10.0f, 0.0f, 0.0f, 0.0f};
        FloatBuffer ambient_metal = FloatBuffer.wrap(noir);
        FloatBuffer diffuse_metal = FloatBuffer.wrap(noirCasse);
        FloatBuffer specular_metal = FloatBuffer.wrap(blancCasse);
        FloatBuffer shininess_metal = FloatBuffer.wrap(shine_low);

        //End

        //Quadric for all the glu models
        GLUquadric quadric = glu.gluNewQuadric();
        int ptr = gl.glGenLists(4);

        // Metal material display list
        int MATTER_METAL = ptr;
        gl.glNewList(MATTER_METAL, GL.GL_COMPILE);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, ambient_metal);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, diffuse_metal);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, specular_metal);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, shininess_metal);
        gl.glEndList();
        //Fin

        for (CompatibilityModeler cis : modelClasses[CLASS_NODE].getModelers()) {
            int newPtr = cis.initDisplayLists(gl, glu, quadric, ptr);
            ptr = newPtr;
        }

        modelClasses[CLASS_POTATO].getCurrentModeler().initDisplayLists(gl, glu, quadric, ptr);

        //Fin

        // Sphere with a texture
        //SHAPE_BILLBOARD = SHAPE_SPHERE32 + 1;
		/*gl.glNewList(SHAPE_BILLBOARD,GL.GL_COMPILE);
        textures[0].bind();
        gl.glBegin(GL.GL_TRIANGLE_STRIP);
        // Map the texture and create the vertices for the particle.
        gl.glTexCoord2d(1, 1);
        gl.glVertex3f(0.5f, 0.5f, 0);
        gl.glTexCoord2d(0, 1);
        gl.glVertex3f(-0.5f, 0.5f,0);
        gl.glTexCoord2d(1, 0);
        gl.glVertex3f(0.5f, -0.5f, 0);
        gl.glTexCoord2d(0, 0);
        gl.glVertex3f(-0.5f,-0.5f, 0);
        gl.glEnd();

        gl.glBindTexture(GL.GL_TEXTURE_2D,0);
        gl.glEndList();*/
        //Fin

        glu.gluDeleteQuadric(quadric);
    }

    public void initObject3dClass() {
        modelClasses = modelClassLibrary.createModelClassesCompatibility(this);
        lodClasses = new CompatibilityModelClass[0];
        selectableClasses = new CompatibilityModelClass[0];
        clickableClasses = new CompatibilityModelClass[0];


        modelClasses[0].setEnabled(true);
        modelClasses[1].setEnabled(false);
        modelClasses[2].setEnabled(true);
        modelClasses[3].setEnabled(true);

        //LOD
        ArrayList<ModelClass> classList = new ArrayList<ModelClass>();
        for (ModelClass objClass : modelClasses) {
            if (objClass.isLod()) {
                classList.add(objClass);
            }
        }
        lodClasses = classList.toArray(lodClasses);

        //Selectable
        classList.clear();
        for (ModelClass objClass : modelClasses) {
            if (objClass.isSelectable()) {
                classList.add(objClass);
            }
        }
        selectableClasses = classList.toArray(selectableClasses);

        //Clickable
        classList.clear();
        for (ModelClass objClass : modelClasses) {
            if (objClass.isClickable()) {
                classList.add(objClass);
            }
        }
        clickableClasses = classList.toArray(clickableClasses);

        //Init selection lists
        selectedObjects = new ConcurrentLinkedQueue[selectableClasses.length];
        int i = 0;
        for (ModelClass objClass : selectableClasses) {
            objClass.setSelectionId(i);
            selectedObjects[i] = new ConcurrentLinkedQueue<ModelImpl>();
            i++;
        }
    }

    @Override
    public void startAnimating() {
        if (!scheduler.isAnimating()) {
            System.out.println("start animating");
            scheduler.start();
            graphIO.startMouseListening();
        }
    }

    @Override
    public void stopAnimating() {
        if (scheduler.isAnimating()) {
            System.out.println("stop animating");
            scheduler.stop();
            graphIO.stopMouseListening();
        }

    }

    public CompatibilityModelClass[] getObject3dClasses() {
        return modelClasses;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }
}
