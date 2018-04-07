package com.skyline.rim.activitys;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.skyline.rim.adapter.NodeTreeAdapter;
import com.skyline.rim.controls.TreeNode;
import com.skyline.rim.controls.TreeNodeHelper;
import com.skyline.rim.controls.TreeNodeLayer;
import com.skyline.teapi.IProjectTree;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.ItemCode;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.UI;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.skyline.rim.globaldata.ConstData.ITEM_CONTOUR_MAP;
import static com.skyline.rim.globaldata.ConstData.ITEM_GROUP;
import static com.skyline.rim.globaldata.ConstData.ITEM_LAYER;
import static com.skyline.rim.globaldata.ConstData.ITEM_LOCATION;
import static com.skyline.rim.globaldata.ConstData.ITEM_OBJECTS;
import static com.skyline.rim.globaldata.ConstData.ITEM_PRESENTATION;
import static com.skyline.rim.globaldata.ConstData.ITEM_SLOPE_MAP;

/**
 * Created by mft on 2017/9/2.
 * skyline信息树界面
 * @description
 */
public class TeTreeActivity extends Activity {
    //信息树控件
    private ListView mListView;
    private NodeTreeAdapter mAdapter;//适配器
    //记录信息树节点的链表
    private LinkedList<TreeNode> mLinkedList = new LinkedList<>();
    //记录信息树节点的列表
    List<TreeNode> data = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_dept_layout);
        mListView = (ListView) findViewById(R.id.id_tree);
        mAdapter = new NodeTreeAdapter(this, mListView, mLinkedList);
        mListView.setAdapter(mAdapter);
        initData();
    }

    /***
     * 初始化skyling信息树
     */
    private void initData() {

        //读取skyline信息树的内容到data
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                readLayers();
            }
        });

        //把data中的节点信息绑定到控件并显示出来
        mLinkedList.addAll(TreeNodeHelper.sortNodes(data));
        mAdapter.notifyDataSetChanged();
    }

    /***
     * 按组读取skyline信息树的子节点信息
     * 特殊声明：此函数为递归函数
     * @param groupID
     * @param parentID
     */
    private void readGroup(String groupID, String parentID) {
        String name = "信息树";
        int visible;
        IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
        if (projectTree.IsGroup(groupID) && projectTree.IsLayer(groupID) == false) {
            try {
                name = projectTree.GetItemName(groupID);
                visible = projectTree.GetVisibility(groupID);
                //ITerraExplorerObject object = projectTree.GetObject(groupID);
                data.add(new TreeNodeLayer(groupID, parentID, name, visible, ITEM_GROUP));
                //Log.i("tsdi",""+name+"type——group="+object.getObjectType());
            } catch (Exception e) {
                Log.i("tsdi", e.getMessage() + name);
            }
        } else {
            name = projectTree.GetItemName(groupID);
            visible = projectTree.GetVisibility(groupID);
            ITerraExplorerObject object = projectTree.GetObject(groupID);
            if (object.getObjectType() == ObjectTypeCode.OT_3D_MESH_LAYER ||
                    object.getObjectType() == ObjectTypeCode.OT_FEATURE_LAYER ||
                    object.getObjectType() == ObjectTypeCode.OT_IMAGERY_LAYER ||
                    object.getObjectType() == ObjectTypeCode.OT_KML_LAYER ||
                    object.getObjectType() == ObjectTypeCode.OT_POINT_CLOUD ||
                    object.getObjectType() == ObjectTypeCode.OT_ELEVATION_LAYER)
                data.add(new TreeNodeLayer(groupID, parentID, name, visible, ITEM_LAYER));
            else data.add(new TreeNodeLayer(groupID, parentID, name, visible, ITEM_GROUP));
            Log.i("tsdi", "" + name + "typeLayer=" + object.getObjectType());
        }
        String itemID = projectTree.GetNextItem(groupID, ItemCode.CHILD);
        while (itemID.equals("") == false) {
            ITerraExplorerObject object = projectTree.GetObject(itemID);
            if (object == null)//如果是group
            {
                if (projectTree.IsGroup(itemID) && projectTree.IsLayer(itemID) == false)
                    readGroup(itemID, groupID);
            } else {
                TreeNodeLayer layer = null;
                visible = projectTree.GetVisibility(itemID);
                name = projectTree.GetItemName(itemID);
                if (object.getObjectType() == ObjectTypeCode.OT_3D_MESH_LAYER ||
                        object.getObjectType() == ObjectTypeCode.OT_FEATURE_LAYER ||
                        object.getObjectType() == ObjectTypeCode.OT_IMAGERY_LAYER ||
                        object.getObjectType() == ObjectTypeCode.OT_KML_LAYER ||
                        object.getObjectType() == ObjectTypeCode.OT_POINT_CLOUD ||
                        object.getObjectType() == ObjectTypeCode.OT_ELEVATION_LAYER) {//如果是图层
                    readGroup(itemID, groupID);
                } else if (object.getObjectType() == ObjectTypeCode.OT_LOCATION) {//位置对象
                    layer = new TreeNodeLayer(itemID, groupID, name, visible, ITEM_LOCATION);
                } else if (object.getObjectType() == ObjectTypeCode.OT_PRESENTATION) {//演示对象
                    layer = new TreeNodeLayer(itemID, groupID, name, visible, ITEM_PRESENTATION);
                } else if (object.getObjectType() == ObjectTypeCode.OT_CONTOUR_MAP) {//
                    layer = new TreeNodeLayer(itemID, groupID, name, visible, ITEM_CONTOUR_MAP);
                } else if (object.getObjectType() == ObjectTypeCode.OT_SLOPE_MAP) {//
                    layer = new TreeNodeLayer(itemID, groupID, name, visible, ITEM_SLOPE_MAP);
                } else {
                    layer = new TreeNodeLayer(itemID, groupID, name, visible, ITEM_OBJECTS);
                    Log.i("tsdi", "" + name + "typeObject=" + object.getObjectType());
                }
                if (layer != null) {
                    data.add(layer);
                }
            }
            itemID = projectTree.GetNextItem(itemID, ItemCode.NEXT);
        }
    }

    /***
     * 读取skyline信息树的全部节点内容
     */
    private void readLayers() {
        // walk over TE tree and find all location and presentation
        IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
        ArrayList<String> groupsToProcess = new ArrayList<String>();
        readGroup(projectTree.getRootID(), "0");
    }

}
