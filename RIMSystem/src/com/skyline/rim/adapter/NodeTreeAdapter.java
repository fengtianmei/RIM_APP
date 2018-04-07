package com.skyline.rim.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.skyline.rim.controls.TreeNode;
import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.UI;

import java.util.LinkedList;
import java.util.List;

import static com.skyline.rim.TeUtils.TeBase.flyToObject;
import static com.skyline.rim.globaldata.ConstData.ITEM_CONTOUR_MAP;
import static com.skyline.rim.globaldata.ConstData.ITEM_GROUP;
import static com.skyline.rim.globaldata.ConstData.ITEM_LAYER;
import static com.skyline.rim.globaldata.ConstData.ITEM_LOCATION;
import static com.skyline.rim.globaldata.ConstData.ITEM_OBJECTS;
import static com.skyline.rim.globaldata.ConstData.ITEM_PRESENTATION;
import static com.skyline.rim.globaldata.ConstData.ITEM_SLOPE_MAP;

/**
 * Created by  mft on 2017/8/2.
 *
 * @description 适配器类，就是listview最常见的适配器写法
 */
public class NodeTreeAdapter extends BaseAdapter {

    //大家经常用ArrayList，但是这里为什么要使用LinkedList
    // ，后面大家会发现因为这个list会随着用户展开、收缩某一项而频繁的进行增加、删除元素操作，
    // 因为ArrayList是数组实现的，频繁的增删性能低下，而LinkedList是链表实现的，对于频繁的增删
    //操作性能要比ArrayList好。
    private LinkedList<TreeNode> nodeLinkedList;
    private LayoutInflater inflater;
    private int retract;//缩进值
    private Context context;
    long mLastTime;
    long mCurTime;

    public NodeTreeAdapter(Context context, ListView listView, LinkedList<TreeNode> linkedList) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        nodeLinkedList = linkedList;
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                expandOrCollapse(position);
//            }
//        });
        //缩进值，大家可以将它配置在资源文件中，从而实现适配
        retract = (int) (context.getResources().getDisplayMetrics().density * 15 + 0.5f);
    }

    /***
     * 反向设置树节点的显示状态，需要考虑子节点以及前辈节点的状态变化
     * @param position 节点位置
     */
    private void invertNodeVisible(int position) {
        TreeNode node = nodeLinkedList.get(position);
        int visibleStatus = node.getstatus() > 0 ? 0 : 1;
        if (node != null)
            if (!node.isLeaf()) {//有子节点
                List<TreeNode> nodeList = node.get_childrenList();
                int size = nodeList.size();
                TreeNode tmp = null;
                for (int i = 0; i < size; i++) {
                    tmp = nodeList.get(i);
                    setNodeVisible(tmp, visibleStatus);
                }
            }
        node.setstatus(visibleStatus);
        setParentNodeVisible(node, visibleStatus);
        notifyDataSetChanged();
    }

    /***
     * 递归设置父节点的显示状态
     * @param node
     * @param visibleStatus
     */
    private void setParentNodeVisible(TreeNode node, int visibleStatus) {
        TreeNode parentNode = node.get_parent();
        if (parentNode != null) {
            List<TreeNode> nodeList = parentNode.get_childrenList();
            int size = nodeList.size();
            TreeNode tmp = null;
            parentNode.setstatus(visibleStatus);
            for (int i = 0; i < size; i++) {
                tmp = nodeList.get(i);
                if (tmp.getstatus() != visibleStatus) {
                    parentNode.setstatus(2);
                    break;
                }
            }
            setParentNodeVisible(parentNode, visibleStatus);
        }
    }

    /***
     * 递归设置子节点的显示状态
     * @param node
     * @param visibleStatus
     */
    private void setNodeVisible(final TreeNode node, int visibleStatus) {
        if (!node.isLeaf()) {//有子节点
            List<TreeNode> nodeList = node.get_childrenList();
            int size = nodeList.size();
            TreeNode tmp = null;
            for (int i = 0; i < size; i++) {
                tmp = nodeList.get(i);
                setNodeVisible(tmp, visibleStatus);
            }
        }
        if (node.getstatus() != visibleStatus) {
            node.setstatus(visibleStatus);
            final boolean visible = visibleStatus == 0 ? false : true;
            // otherwise toggle visibility
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    ISGWorld.getInstance().getProjectTree().SetVisibility((String) node.get_id(), visible);
                }
            });
        }
    }

    /**
     * 展开或收缩用户点击的条目
     *
     * @param position
     */
    private void expandOrCollapse(int position) {
        TreeNode node = nodeLinkedList.get(position);
        if (node != null && !node.isLeaf()) {
            boolean old = node.isExpand();
            if (old) {
                List<TreeNode> nodeList = node.get_childrenList();
                int size = nodeList.size();
                TreeNode tmp = null;
                for (int i = 0; i < size; i++) {
                    tmp = nodeList.get(i);
                    if (tmp.isExpand()) {
                        collapse(tmp, position + 1);
                    }
                    nodeLinkedList.remove(position + 1);
                }
            } else {
                nodeLinkedList.addAll(position + 1, node.get_childrenList());
            }
            node.setIsExpand(!old);
            notifyDataSetChanged();
        }
    }

    /**
     * 递归收缩用户点击的条目
     * 因为此中实现思路是：当用户展开某一条时，就将该条对应的所有子节点加入到nodeLinkedList
     * ，同时控制缩进，当用户收缩某一条时，就将该条所对应的子节点全部删除，而当用户跨级缩进时
     * ，就需要递归缩进其所有的孩子节点，这样才能保持整个nodeLinkedList的正确性，同时这种实
     * 现方式避免了每次对所有数据进行处理然后插入到一个list，最后显示出来，当数据量一大，就会卡顿，
     * 所以这种只改变局部数据的方式性能大大提高。
     *
     * @param position
     */
    private void collapse(TreeNode node, int position) {
        node.setIsExpand(false);
        List<TreeNode> nodes = node.get_childrenList();
        int size = nodes.size();
        TreeNode tmp = null;
        for (int i = 0; i < size; i++) {
            tmp = nodes.get(i);
            if (tmp.isExpand()) {
                collapse(tmp, position + 1);
            }
            nodeLinkedList.remove(position + 1);
        }
    }

    @Override
    public int getCount() {
        return nodeLinkedList.size();
    }

    @Override
    public Object getItem(int position) {
        return nodeLinkedList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /***
     * 适配器标准函数，重写以实现消息处理、设置图标等功能
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.tree_listview_item, null);
            holder = new ViewHolder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.id_treenode_icon);
            holder.label = (TextView) convertView.findViewById(R.id.id_treenode_label);
            holder.confirm = (LinearLayout) convertView.findViewById(R.id.id_confirm);
            holder.imageVisible = (ImageView) convertView.findViewById(R.id.id_visible);
            holder.imageNodeType = (ImageView) convertView.findViewById(R.id.id_nodetype_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        TreeNode node = nodeLinkedList.get(position);
        holder.label.setText(node.get_label());

        holder.label.setTag(position);
        holder.label.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TreeNode node = nodeLinkedList.get((int) v.getTag());
                //Toast.makeText(context,"选中:"+node.get_id(),Toast.LENGTH_SHORT).show();
                final String objID = (String) node.get_id();
                Activity activity = (Activity) context;
                activity.finish();
                //fly to object
                flyToObject(objID);
                return false;
            }
        });

//        holder.label.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mLastTime = mCurTime;
//                mCurTime = System.currentTimeMillis();
//                if (mCurTime - mLastTime < 500) {//双击事件
//                    mCurTime = 0;
//                    mLastTime = 0;
//                    TreeNode node = nodeLinkedList.get((int) v.getTag());
//                    //Toast.makeText(context,"选中:"+node.get_id(),Toast.LENGTH_SHORT).show();
//                    final String objID = (String) node.get_id();
//                    Activity activity = (Activity) context;
//                    activity.finish();
//                    //fly to object
//                    flyToObject(objID);
//                } else {//单击事件
//                    Log.i("tsdi", "click");
//                }
//            }
//        });

        if (node.get_icon() == -1) {
            holder.imageView.setVisibility(View.INVISIBLE);
        } else {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(node.get_icon());
        }

        setNodeTypeImage(holder.imageNodeType, node.getnodetype());
        setVisibleImage(holder.imageVisible, node.getstatus(), node.getnodetype());
        holder.confirm.setTag(position);
        holder.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TreeNode node = nodeLinkedList.get((int)v.getTag());
                //Toast.makeText(context,"选中:"+node.get_id(),Toast.LENGTH_SHORT).show();
                invertNodeVisible((int) v.getTag());
            }
        });

        holder.imageView.setTag(position);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandOrCollapse((int) v.getTag());
            }
        });

        convertView.setPadding(node.get_level() * retract, 5, 5, 5);
        return convertView;
    }

    /***
     * 根据节点类型设置节点的图标
     * @param imageview
     * @param type
     */
    private void setNodeTypeImage(ImageView imageview, int type) {
        switch (type) {
            case ITEM_GROUP:
                imageview.setImageResource(R.drawable.foder);
                break;
            case ITEM_LAYER:
                imageview.setImageResource(R.drawable.layer);
                break;
            case ITEM_OBJECTS:
                imageview.setImageResource(R.drawable.model);
                break;
            case ITEM_LOCATION:
                imageview.setImageResource(R.drawable.minilocate);
                break;
            case ITEM_PRESENTATION:
                imageview.setImageResource(R.drawable.minipresentation);
                break;
            default:
                imageview.setImageResource(R.drawable.model);
                break;
        }
    }

    /***
     * 根据节点显示状态，设置节点的图标
     * @param imageview
     * @param type
     * @param nodeType
     */
    private void setVisibleImage(ImageView imageview, int type, int nodeType) {
        if (nodeType == ITEM_LOCATION || nodeType == ITEM_PRESENTATION || nodeType == ITEM_CONTOUR_MAP || nodeType == ITEM_SLOPE_MAP) {
            imageview.setImageResource(R.drawable.nullimage);
            return;
        }
        switch (type) {
            case 0:
                imageview.setImageResource(R.drawable.checkbox_off);
                break;
            case 1:
                imageview.setImageResource(R.drawable.checkbox_on);
                break;
            case 2:
                imageview.setImageResource(R.drawable.checkbox_semi);
                break;
            default:
                break;
        }
    }

    static class ViewHolder {
        public ImageView imageView;
        public ImageView imageNodeType;
        public TextView label;
        public LinearLayout confirm;
        public ImageView imageVisible;
    }

}
