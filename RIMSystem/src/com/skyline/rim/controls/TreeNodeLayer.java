package com.skyline.rim.controls;

/**
 * Created by mft on 2017/8/2.
 *
 * @description skyline信息树节点类（继承Node），此处的泛型Integer是因为ID和parentID都为int
 * ，如果为String传入泛型String即可
 */

public class TreeNodeLayer extends TreeNode<String> {
    private String id;//节点ID
    private String parentId;//父亲节点ID
    private String name;//节点名称
    private int nodetype;//节点类型，记录节点上skyline object 的类型
    private int status = 0;//0=不显示  1=显示 2=不全显示（针对文件夹）

    public TreeNodeLayer() {
    }

    public TreeNodeLayer(String id, String parentId, String name, int status, int nodetype) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.status = status;
        this.nodetype = nodetype;
    }

    /**
     * 此处返回节点ID
     *
     * @return
     */
    @Override
    public String get_id() {
        return id;
    }

    /**
     * 此处返回父亲节点ID
     *
     * @return
     */
    @Override
    public String get_parentId() {
        return parentId;
    }

    @Override
    public String get_label() {
        return name;
    }

    @Override
    public boolean parent(TreeNode dest) {
        if (id.equals(dest.get_parentId())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean child(TreeNode dest) {

        if (parentId.equals(dest.get_id())) {
            return true;
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int getnodetype() {
        return nodetype;
    }

    @Override
    public int getstatus() {
        return status;
    }

    @Override
    public void setstatus(int status) {
        this.status = status;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
