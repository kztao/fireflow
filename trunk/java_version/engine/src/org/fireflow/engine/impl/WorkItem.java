/**
 * Copyright 2007-2008 非也
 * All rights reserved. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation。
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses. *
 */
package org.fireflow.engine.impl;

// Generated Feb 23, 2008 12:04:21 AM by Hibernate Tools 3.2.0.b9
import java.util.Date;
import java.util.List;

import org.fireflow.engine.EngineException;
import org.fireflow.engine.IRuntimeContextAware;
import org.fireflow.engine.ITaskInstance;
import org.fireflow.engine.IWorkItem;
import org.fireflow.engine.IWorkflowSession;
import org.fireflow.engine.IWorkflowSessionAware;
import org.fireflow.engine.RuntimeContext;
import org.fireflow.engine.taskinstance.DynamicAssignmentHandler;
import org.fireflow.engine.taskinstance.ITaskInstanceManager;
import org.fireflow.kernel.IActivityInstance;
import org.fireflow.kernel.KernelException;
import org.fireflow.model.Task;
import org.fireflow.model.net.Activity;
import org.fireflow.model.net.Synchronizer;
import org.fireflow.model.net.Transition;

/**
 * WorkItem generated by hbm2java
 */
public class WorkItem implements IWorkItem, IRuntimeContextAware, IWorkflowSessionAware, java.io.Serializable {

    private String actorId;
    private String id;
    private Integer state = null;
    private Date createdTime;
    /**
     * 签收时间
     */
    private Date claimedTime;
    private Date endTime;
    private String comments;
    private ITaskInstance taskInstance;
    protected transient RuntimeContext rtCtx = null;
    protected transient IWorkflowSession workflowSession = null;

    public void setRuntimeContext(RuntimeContext ctx) {
        this.rtCtx = ctx;
        if (this.taskInstance!=null){
        	((IRuntimeContextAware)taskInstance).setRuntimeContext(this.rtCtx);
        }
    }

    public RuntimeContext getRuntimeContext() {
        return this.rtCtx;
    }

    public WorkItem() {
    }

    public WorkItem(TaskInstance taskInstance) {
        this.taskInstance = taskInstance;
    }

    public WorkItem(Integer state, Date createdTime, Date signedTm,
            Date endTime, String comments, TaskInstance taskInstance) {
        this.state = state;
        this.createdTime = createdTime;
        this.claimedTime = signedTm;
        this.endTime = endTime;
        this.comments = comments;
        this.taskInstance = taskInstance;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getState() {
        return this.state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Date getCreatedTime() {
        return this.createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    /**
     * @deprecated 
     * @return
     */
    public Date getSignedTime() {
        return this.claimedTime;
    }

    public Date getClaimedTime() {
        return this.claimedTime;
    }

    /**
     * @deprecated
     * @param acceptedTime
     */
    public void setSignedTime(Date claimedTime) {
        this.claimedTime = claimedTime;
    }

    public void setClaimedTime(Date claimedTime) {
        this.claimedTime = claimedTime;
    }

    public Date getEndTime() {
        return this.endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getComments() {
        return this.comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public ITaskInstance getTaskInstance() {
        return this.taskInstance;
    }

    public void setTaskInstance(ITaskInstance taskInstance) {
        this.taskInstance = taskInstance;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public IWorkItem withdraw() throws EngineException, KernelException {
    	if (this.workflowSession==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current workflow session is null.");
    	}
    	if (this.rtCtx==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current runtime context is null.");    		
    	}    	
        ITaskInstanceManager taskInstanceMgr = this.rtCtx.getTaskInstanceManager();
        return taskInstanceMgr.withdrawWorkItem(this);
    }


    public void reject()throws EngineException, KernelException{
        reject(this.getComments());
    }
    
    public void reject(String comments) throws EngineException, KernelException {
    	if (this.workflowSession==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current workflow session is null.");
    	}
    	if (this.rtCtx==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current runtime context is null.");    		
    	}    	
        ITaskInstanceManager taskInstanceMgr = this.rtCtx.getTaskInstanceManager();
        taskInstanceMgr.rejectWorkItem(this,comments);
    }
    
    public void complete() throws EngineException, KernelException {
        complete(null,this.getComments());
    }

    public void complete(String comments) throws EngineException, KernelException {
        complete(null,comments);
    }



	public void complete(DynamicAssignmentHandler dynamicAssignmentHandler, String comments)
			throws EngineException, KernelException {
    	if (this.workflowSession==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current workflow session is null.");
    	}
    	if (this.rtCtx==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current runtime context is null.");    		
    	}
    	if (dynamicAssignmentHandler!=null){
    		this.workflowSession.setDynamicAssignmentHandler(dynamicAssignmentHandler);
    	}
        ITaskInstanceManager taskInstanceManager = this.rtCtx.getTaskInstanceManager();
        taskInstanceManager.completeWorkItem(this, null,comments);
	}
	
    public IWorkItem reasignTo(String actorId) throws EngineException{
        return reasignTo(actorId, this.getComments());
    }

    public IWorkItem reasignTo(String actorId, String comments) throws EngineException{
    	if (this.workflowSession==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current workflow session is null.");
    	}
    	if (this.rtCtx==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current runtime context is null.");    		
    	}
  	
        ITaskInstanceManager manager = this.rtCtx.getTaskInstanceManager();
        return manager.reasignWorkItemTo(this, actorId, comments);
    }

    /**
     * @deprecated 
     * @throws org.fireflow.engine.EngineException
     * @throws org.fireflow.kenel.KenelException
     */
    public void sign() throws EngineException, KernelException {
        claim();
    }

    /**
     * 签收
     * @throws org.fireflow.engine.EngineException
     * @throws org.fireflow.kenel.KenelException
     */
    public IWorkItem claim() throws EngineException, KernelException {
    	if (this.workflowSession==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current workflow session is null.");
    	}
    	if (this.rtCtx==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current runtime context is null.");    		
    	}
	
    	
        ITaskInstanceManager taskInstanceMgr = rtCtx.getTaskInstanceManager();
        IWorkItem newWorkItem = taskInstanceMgr.claimWorkItem(this.getId(),this.getTaskInstance().getId());
        
        if (newWorkItem!=null){
        	this.state = newWorkItem.getState();
        	this.claimedTime = newWorkItem.getClaimedTime();
        	
        	((IRuntimeContextAware)newWorkItem).setRuntimeContext(rtCtx);
        	((IWorkflowSessionAware)newWorkItem).setCurrentWorkflowSession(this.workflowSession);
        }else{
        	this.state = IWorkItem.CANCELED;
        }
        
        return newWorkItem;
    }



    public void jumpTo(String activityId) throws EngineException, KernelException {
        jumpTo(activityId, null, this.getComments());
    }

    public void jumpTo(String activityId, String comments) throws EngineException, KernelException {
        jumpTo(activityId, null, comments);
    }

 

    public void jumpTo(String targetActivityId, DynamicAssignmentHandler dynamicAssignmentHandler, String comments) throws EngineException, KernelException {
    	if (this.workflowSession==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current workflow session is null.");
    	}
    	if (this.rtCtx==null){
    		new EngineException(this.getTaskInstance().getProcessInstanceId(),
    				this.getTaskInstance().getWorkflowProcess(),this.getTaskInstance().getTaskId(),
    				"The current runtime context is null.");    		
    	}
    	if (dynamicAssignmentHandler!=null){
    		this.workflowSession.setDynamicAssignmentHandler(dynamicAssignmentHandler);
    	}
        ITaskInstanceManager taskInstanceManager = this.rtCtx.getTaskInstanceManager();
        taskInstanceManager.completeWorkItemAndJumpTo(this, targetActivityId,comments);
    }

    public IWorkflowSession getCurrentWorkflowSession() {
        return this.workflowSession;
    }

    public void setCurrentWorkflowSession(IWorkflowSession session) {
        this.workflowSession = session;
        if (this.taskInstance!=null){
        	((IWorkflowSessionAware)taskInstance).setCurrentWorkflowSession(this.workflowSession);
        }
    }
 
}