package com.skyline.rim.TeUtils;

import android.util.Log;

import com.skyline.teapi.ActionCode;
import com.skyline.teapi.ApiException;
import com.skyline.teapi.IPresentation;
import com.skyline.teapi.IProjectTree;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.ItemCode;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.UI;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * 一些操作te的基础函数库
 * Created by mft on 2017/8/28.
 */

public class TeBase {
    /***
     * 飞到目标
     * @param objID 目标ID
     */
    public static void flyToObject(final String objID) {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                try {
                    IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
                    ITerraExplorerObject object = projectTree.GetObject(objID);
                    if (object.getObjectType() == ObjectTypeCode.OT_PRESENTATION) {
                        IPresentation currentPresentation = null;
                        try {
                            currentPresentation = UI.runOnRenderThread(new Callable<IPresentation>() {

                                @Override
                                public IPresentation call() throws Exception {
                                    return ISGWorld.getInstance().getCreator().GetObject(objID).CastTo(IPresentation.class);
                                }
                            });
                            if (currentPresentation != null) {
                                currentPresentation.Stop();
                                currentPresentation.Play(0);
                            }
                        } catch (ApiException e) {
                            return;
                        }
                    }
                    ISGWorld.getInstance().getNavigate().FlyTo(objID, ActionCode.AC_FLYTO);
                } catch (Exception e) {
                    Log.i("tsdi", e.getMessage());
                }
            }
        });
    }

    /***
     * 飞到目标，飞行到第一个匹配上的目标
     * @param name 目标名字
     */
    public static void flyToObjectByName(final String name) {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                try {
                    IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
                    String itemid = projectTree.FindItem(name);
                    if (itemid == null || itemid.length() == 0)
                        return;
                    flyToObject(itemid);
                } catch (Exception e) {
                    Log.i("tsdi", e.getMessage());
                }
            }
        });
    }

    /***
     * 飞到目标桥，飞行到第一个匹配上的目标桥
     * @param name 桥的名字
     */
    public static void flyToBridge(final String name) {
        flyToObjectByName("桥梁\\" + name + "_point");
    }

    /***
     * 飞到目标隧道，飞行到第一个匹配上的目标隧道
     * @param name 隧道的名字
     */
    public static void flyToTunnel(final String name) {
        flyToObjectByName("隧道\\" + name);
    }
    public static void flyToProject() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<String> list = getObjsFromProject(ObjectTypeCode.OT_LOCATION);
                    IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
                    if (list.size() > 0) {
                        String ID = list.get(0);
                        for (int i = 0; i < list.size(); i++) {
                            if (projectTree.GetItemName(list.get(i)).equals(R.string.info_start_play))
                                ID = list.get(i);
                        }
                        ISGWorld.getInstance().getNavigate().FlyTo(list.get(0), ActionCode.AC_FLYTO);
                    }
                } catch (Exception e) {
                    Log.i("tsdi", e.getMessage());
                }
            }
        });
    }

    /***
     * 按组检索莫一类对象
     * @param type
     * @return
     */
    public static ArrayList<String> getObjsFromProject(int type) {
        ArrayList<String> objList = new ArrayList<String>();
        try {
            IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
            getObjects(projectTree.getRootID(), "0", type, objList);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
        return objList;
    }

    /***
     * 不要调用此函数，构建信息树时专用
     * 从组里面按组检索莫一类对象
     * @param groupID
     * @param parentID
     */
    private static void getObjects(String groupID, String parentID, int objType, ArrayList<String> objList) {
        //ObjectTypeCode.OT_PRESENTATION
        try {
            IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
            String itemID = projectTree.GetNextItem(groupID, ItemCode.CHILD);
            while (itemID.equals("") == false) {
                ITerraExplorerObject object = projectTree.GetObject(itemID);
                if (object == null)//如果是group
                {
                    if (projectTree.IsGroup(itemID) && projectTree.IsLayer(itemID) == false)
                        getObjects(itemID, groupID, objType, objList);
                } else if (object.getObjectType() == objType) {//演示对象
                    objList.add(itemID);
                    String itemName = projectTree.GetItemName(itemID);
                }
                itemID = projectTree.GetNextItem(itemID, ItemCode.NEXT);
            }
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
    }







}
