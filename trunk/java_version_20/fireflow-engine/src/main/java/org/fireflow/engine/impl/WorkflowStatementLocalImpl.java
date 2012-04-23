/**
 * Copyright 2007-2010 非也
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

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.fireflow.engine.WorkflowQuery;
import org.fireflow.engine.WorkflowStatement;
import org.fireflow.engine.context.RuntimeContext;
import org.fireflow.engine.entity.EntityProperty;
import org.fireflow.engine.entity.WorkflowEntity;
import org.fireflow.engine.entity.repository.ProcessDescriptor;
import org.fireflow.engine.entity.repository.ProcessDescriptorProperty;
import org.fireflow.engine.entity.repository.ProcessKey;
import org.fireflow.engine.entity.repository.ProcessRepository;
import org.fireflow.engine.entity.repository.ResourceDescriptorProperty;
import org.fireflow.engine.entity.repository.ResourceRepository;
import org.fireflow.engine.entity.repository.ServiceDescriptorProperty;
import org.fireflow.engine.entity.repository.ServiceRepository;
import org.fireflow.engine.entity.runtime.ActivityInstance;
import org.fireflow.engine.entity.runtime.ActivityInstanceState;
import org.fireflow.engine.entity.runtime.ProcessInstance;
import org.fireflow.engine.entity.runtime.ProcessInstanceProperty;
import org.fireflow.engine.entity.runtime.ProcessInstanceState;
import org.fireflow.engine.entity.runtime.ScheduleJob;
import org.fireflow.engine.entity.runtime.Scope;
import org.fireflow.engine.entity.runtime.Variable;
import org.fireflow.engine.entity.runtime.WorkItem;
import org.fireflow.engine.entity.runtime.WorkItemState;
import org.fireflow.engine.entity.runtime.impl.AbsVariable;
import org.fireflow.engine.entity.runtime.impl.ProcessInstanceHistory;
import org.fireflow.engine.entity.runtime.impl.ProcessInstanceImpl;
import org.fireflow.engine.entity.runtime.impl.VariableImpl;
import org.fireflow.engine.exception.InvalidOperationException;
import org.fireflow.engine.exception.WorkflowProcessNotFoundException;
import org.fireflow.engine.invocation.AssignmentHandler;
import org.fireflow.engine.modules.calendar.CalendarService;
import org.fireflow.engine.modules.instancemanager.ActivityInstanceManager;
import org.fireflow.engine.modules.instancemanager.ProcessInstanceManager;
import org.fireflow.engine.modules.loadstrategy.ProcessLoadStrategy;
import org.fireflow.engine.modules.ousystem.User;
import org.fireflow.engine.modules.persistence.PersistenceService;
import org.fireflow.engine.modules.persistence.Persister;
import org.fireflow.engine.modules.persistence.ProcessPersister;
import org.fireflow.engine.modules.persistence.ResourcePersister;
import org.fireflow.engine.modules.persistence.ServicePersister;
import org.fireflow.engine.modules.persistence.VariablePersister;
import org.fireflow.engine.modules.persistence.WorkItemPersister;
import org.fireflow.engine.modules.process.ProcessUtil;
import org.fireflow.engine.modules.workitem.WorkItemManager;
import org.fireflow.model.InvalidModelException;
import org.fireflow.model.data.Property;
import org.fireflow.model.resourcedef.WorkItemAssignmentStrategy;
import org.fireflow.pvm.kernel.KernelManager;
import org.fireflow.pvm.kernel.PObjectKey;
import org.fireflow.pvm.kernel.Token;
import org.firesoa.common.schema.NameSpaces;

/**
 * @author 非也
 * @version 2.0
 */
public class WorkflowStatementLocalImpl implements WorkflowStatement {
	WorkflowSessionLocalImpl session = null;
	Map<String, Object> attributes = new HashMap<String, Object>();
	protected String processType = null;

	public String getProcessType() {
		return processType;
	}

	public void setProcessType(String processType) {
		this.processType = processType;
	}

	public Object execute(StatementCallback callback) {
		session.clearAttributes();
		session.setAllAttributes(attributes);
		return callback.doInStatement(session);
	}

	public WorkflowStatementLocalImpl(WorkflowSessionLocalImpl s) {
		this.session = s;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.WorkflowStatement#setDynamicAssignmentHandler(java
	 * .lang.String, org.fireflow.engine.service.human.AssignmentHandler)
	 */
	public WorkflowStatement setDynamicAssignmentHandler(String activityId,
			AssignmentHandler dynamicAssignmentHandler) {
		this.session.setDynamicAssignmentHandler(activityId,
				dynamicAssignmentHandler);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.api.WorkflowStatement#getCurrentActivityInstance()
	 */
	public ActivityInstance getCurrentActivityInstance() {
		return this.session.getCurrentActivityInstance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.api.WorkflowStatement#getCurrentProcessInstance()
	 */
	public ProcessInstance getCurrentProcessInstance() {
		return this.session.getCurrentProcessInstance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.api.WorkflowStatement#getLatestCreatedWorkItems()
	 */
	public List<WorkItem> getLatestCreatedWorkItems() {
		return this.session.getLatestCreatedWorkItems();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.api.WorkflowStatement#setAttribute(java.lang.String,
	 * java.lang.Object)
	 */
	public WorkflowStatement setAttribute(String name, Object attr) {
		attributes.put(name, attr);
		return this;
	}

	/******************************************************************************/
	/************                                                        **********/
	/************ 与process instance 相关的API **********/
	/************                                                        **********/
	/************                                                        **********/
	/******************************************************************************/

	public ProcessInstance startProcess(String workflowProcessId, int version,
			String bizId, Map<String, Object> variables)
			throws InvalidModelException, WorkflowProcessNotFoundException,
			InvalidOperationException {
		RuntimeContext ctx = this.session.getRuntimeContext();
//		ProcessInstanceManager procInstMgr = ctx.getEngineModule(
//				ProcessInstanceManager.class, this.processType);
//
//		ProcessInstance processInstance = procInstMgr.startProcess(session, workflowProcessId, version,
//				this.processType, bizId, variables);

		ProcessUtil processUtil = ctx.getEngineModule(ProcessUtil.class, processType);
		
		session.setAttribute(InternalSessionAttributeKeys.BIZ_ID, bizId);
		session.setAttribute(InternalSessionAttributeKeys.VARIABLES, variables);
		RuntimeContext context = ((WorkflowSessionLocalImpl)session).getRuntimeContext();
		KernelManager kernelManager = context.getDefaultEngineModule(KernelManager.class);			
		//启动WorkflowProcess实际上是启动该WorkflowProcess的main_flow，
		kernelManager.startPObject(session, new PObjectKey(workflowProcessId,version,processType,
				processUtil.getProcessEntryId(workflowProcessId, version, workflowProcessId)));
		
		return session.getCurrentProcessInstance();
//
//		
//		return processInstance;
	}

	public ProcessInstance startProcess(String workflowProcessId, String bizId,
			Map<String, Object> variables) throws InvalidModelException,
			WorkflowProcessNotFoundException, InvalidOperationException {
		// 首先需要根据workflowProcessId找到待启动的流程，查找策略有多种，可能根据流程族来查找，也可能直接找到当前最新版本的流程。
		RuntimeContext runtimeContext = this.session.getRuntimeContext();
		ProcessLoadStrategy loadStrategy = runtimeContext.getEngineModule(
				ProcessLoadStrategy.class, this.getProcessType());

		ProcessKey pk = loadStrategy.findTheProcessKeyForRunning(session,
				workflowProcessId, this.getProcessType());
		return this.startProcess(workflowProcessId, pk.getVersion(), bizId,
				variables);
	}

	public ProcessInstance startProcess(Object process, String bizId,
			Map<String, Object> variables) throws InvalidModelException,
			WorkflowProcessNotFoundException, InvalidOperationException {

		ProcessRepository repository = this.uploadProcess(process, Boolean.TRUE, null);
		return this.startProcess(repository.getProcessId(),
				repository.getVersion(), bizId, variables);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.WorkflowStatement#abortProcessInstance(java.lang.
	 * String, java.lang.String)
	 */
	public ProcessInstance abortProcessInstance(String processInstanceId,
			String note) throws InvalidOperationException {	
		RuntimeContext ctx = this.session.getRuntimeContext();
		ProcessInstance processInstance = this.getEntity(processInstanceId,
				ProcessInstance.class);
		if (processInstance == null) {
			throw new InvalidOperationException("Process instance for id="
					+ processInstanceId + " is not found.");
		}
		if (processInstance.getState().getValue() > ProcessInstanceState.DELIMITER
				.getValue()
				|| processInstance instanceof ProcessInstanceHistory) {
			throw new InvalidOperationException("Process instance for id="
					+ processInstanceId + " is dead.");
		}
		
		
		resetSession(this.session);//先清理session
		if (!StringUtils.isEmpty(note)){
			Map<EntityProperty,Object> fieldsValues = new HashMap<EntityProperty,Object>();
			fieldsValues.put(ProcessInstanceProperty.NOTE, note);
			session.setAttribute(InternalSessionAttributeKeys.FIELDS_VALUES, fieldsValues);
		}
		session.setCurrentProcessInstance(processInstance);	
		
		KernelManager kernelManager = ctx.getDefaultEngineModule(KernelManager.class);		
		Token token = kernelManager.getToken(processInstance.getTokenId(), processInstance.getProcessType());

		kernelManager.fireTerminationEvent(session, token, null);
		kernelManager.execute(session);
		
		return processInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.WorkflowStatement#restoreProcessInstance(java.lang
	 * .String, java.lang.String)
	 */
	public ProcessInstance restoreProcessInstance(String processInstanceId,
			String note) throws InvalidOperationException {	
		RuntimeContext ctx = this.session.getRuntimeContext();
		ProcessInstance processInstance = this.getEntity(processInstanceId,
				ProcessInstance.class);
		if (processInstance == null) {
			throw new InvalidOperationException("Process instance for id="
					+ processInstanceId + " is not found.");
		}
		if (processInstance.getState().getValue() > ProcessInstanceState.DELIMITER
				.getValue()
				|| processInstance instanceof ProcessInstanceHistory) {
			throw new InvalidOperationException("Process instance for id="
					+ processInstanceId + " is dead.");
		}
		
		this.resetSession(session);
		if (!StringUtils.isEmpty(note)){
			Map<EntityProperty,Object> fieldsValues = new HashMap<EntityProperty,Object>();
			fieldsValues.put(ProcessInstanceProperty.NOTE, note);
			session.setAttribute(InternalSessionAttributeKeys.FIELDS_VALUES, fieldsValues);
		}
		session.setCurrentProcessInstance(processInstance);

		ProcessInstanceManager procInstMgr = ctx.getEngineModule(
				ProcessInstanceManager.class, this.processType);
		procInstMgr.restoreProcessInstance(session, processInstance);
		return processInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.WorkflowStatement#suspendProcessInstance(java.lang
	 * .String, java.lang.String)
	 */
	public ProcessInstance suspendProcessInstance(String processInstanceId,
			String note) throws InvalidOperationException {	
		RuntimeContext ctx = this.session.getRuntimeContext();
		ProcessInstance processInstance = this.getEntity(processInstanceId,
				ProcessInstance.class);
		if (processInstance == null) {
			throw new InvalidOperationException("Process instance for id="
					+ processInstanceId + " is not found.");
		}
		if (processInstance.getState().getValue() > ProcessInstanceState.DELIMITER
				.getValue()
				|| processInstance instanceof ProcessInstanceHistory) {
			throw new InvalidOperationException("Process instance for id="
					+ processInstanceId + " is dead.");
		}
		
		this.resetSession(session);
		if (!StringUtils.isEmpty(note)){
			Map<EntityProperty,Object> fieldsValues = new HashMap<EntityProperty,Object>();
			fieldsValues.put(ProcessInstanceProperty.NOTE, note);
			session.setAttribute(InternalSessionAttributeKeys.FIELDS_VALUES, fieldsValues);
		}
		
		session.setCurrentProcessInstance(processInstance);

		ProcessInstanceManager procInstMgr = ctx.getEngineModule(
				ProcessInstanceManager.class, this.processType);
		procInstMgr.suspendProcessInstance(session, processInstance);
		return processInstance;
	}

	/******************************************************************************/
	/************                                                        **********/
	/************ ActivityInstance相关的 API **********/
	/************                                                        **********/
	/************                                                        **********/
	/******************************************************************************/

	public ActivityInstance suspendActivityInstance(String activityInstanceId,
			String note) throws InvalidOperationException {
		RuntimeContext ctx = this.session.getRuntimeContext();
		ActivityInstance activityInstance = this.getEntity(activityInstanceId,
				ActivityInstance.class);
		if (activityInstance == null) {
			throw new InvalidOperationException("Activity instance for id="
					+ activityInstanceId + " is not found.");
		}
		if (activityInstance.getState().getValue() > ActivityInstanceState.DELIMITER
				.getValue()
				|| activityInstance instanceof ProcessInstanceHistory) {
			throw new InvalidOperationException("Activiy instance for id="
					+ activityInstanceId + " is dead.");
		}
		this.resetSession(session);
		if (!StringUtils.isEmpty(note)){
			Map<EntityProperty,Object> fieldsValues = new HashMap<EntityProperty,Object>();
			fieldsValues.put(ProcessInstanceProperty.NOTE, note);
			session.setAttribute(InternalSessionAttributeKeys.FIELDS_VALUES, fieldsValues);
		}
		
		session.setCurrentActivityInstance(activityInstance);

		ActivityInstanceManager actInstMgr = ctx.getEngineModule(
				ActivityInstanceManager.class, this.processType);
		actInstMgr.suspendActivityInstance(session, activityInstance);
		return activityInstance;
	}

	public ActivityInstance restoreActivityInstance(String activityInstanceId,
			String note) throws InvalidOperationException {
		RuntimeContext ctx = this.session.getRuntimeContext();
		ActivityInstance activityInstance = this.getEntity(activityInstanceId,
				ActivityInstance.class);
		if (activityInstance == null) {
			throw new InvalidOperationException("Activity instance for id="
					+ activityInstanceId + " is not found.");
		}
		if (activityInstance.getState().getValue() > ActivityInstanceState.DELIMITER
				.getValue()
				|| activityInstance instanceof ProcessInstanceHistory) {
			throw new InvalidOperationException("Activiy instance for id="
					+ activityInstanceId + " is dead.");
		}
		this.resetSession(session);
		if (!StringUtils.isEmpty(note)){
			Map<EntityProperty,Object> fieldsValues = new HashMap<EntityProperty,Object>();
			fieldsValues.put(ProcessInstanceProperty.NOTE, note);
			session.setAttribute(InternalSessionAttributeKeys.FIELDS_VALUES, fieldsValues);
		}
		
		session.setCurrentActivityInstance(activityInstance);

		ActivityInstanceManager actInstMgr = ctx.getEngineModule(
				ActivityInstanceManager.class, this.processType);
		actInstMgr.restoreActivityInstance(session, activityInstance);
		return activityInstance;
	}

	public ActivityInstance abortActivityInstance(String activityInstanceId,
			String note) throws InvalidOperationException {
		RuntimeContext ctx = this.session.getRuntimeContext();
		ActivityInstance activityInstance = this.getEntity(activityInstanceId,
				ActivityInstance.class);
		if (activityInstance == null) {
			throw new InvalidOperationException("Activity instance for id="
					+ activityInstanceId + " is not found.");
		}
		if (activityInstance.getState().getValue() > ActivityInstanceState.DELIMITER
				.getValue()
				|| activityInstance instanceof ProcessInstanceHistory) {
			throw new InvalidOperationException("Activiy instance for id="
					+ activityInstanceId + " is dead.");
		}
		
		this.resetSession(session);
		if (!StringUtils.isEmpty(note)){
			Map<EntityProperty,Object> fieldsValues = new HashMap<EntityProperty,Object>();
			fieldsValues.put(ProcessInstanceProperty.NOTE, note);
			session.setAttribute(InternalSessionAttributeKeys.FIELDS_VALUES, fieldsValues);
		}
		session.setCurrentActivityInstance(activityInstance);

		
		KernelManager kernelManager = ctx.getDefaultEngineModule(KernelManager.class);		
		Token token = kernelManager.getToken(activityInstance.getTokenId(), activityInstance.getProcessType());

		kernelManager.fireTerminationEvent(session, token, null);
		kernelManager.execute(session);
		
		return activityInstance;
	}

	/******************************************************************************/
	/************                                                        **********/
	/************ workItem 相关的API **********/
	/************                                                        **********/
	/************                                                        **********/
	/******************************************************************************/

	public void updateWorkItem(WorkItem workItem)
			throws InvalidOperationException {
		if (workItem.getState().getValue() != WorkItemState.RUNNING.getValue()) {
			throw new InvalidOperationException(
					"Can not update the work item ,it is not in Running State.");
		}
		RuntimeContext rtCtx = this.session.getRuntimeContext();

		PersistenceService persistenceService = rtCtx.getEngineModule(
				PersistenceService.class, processType);
		WorkItemPersister workItemPersister = persistenceService
				.getWorkItemPersister();
		workItemPersister.saveOrUpdate(workItem);
	}

	public WorkItem claimWorkItem(String workItemId)
			throws InvalidOperationException {
		RuntimeContext rtCtx = this.session.getRuntimeContext();

		PersistenceService persistenceService = rtCtx.getEngineModule(
				PersistenceService.class, processType);
		WorkItemPersister workItemPersister = persistenceService
				.getWorkItemPersister();

		WorkItem workItem = workItemPersister.find(WorkItem.class, workItemId);

		return claimWorkItem(workItem);
	}

	private WorkItem claimWorkItem(WorkItem workItem)
			throws InvalidOperationException {
		RuntimeContext rtCtx = this.session.getRuntimeContext();
		if (workItem == null)
			throw new InvalidOperationException(
					"Claim work item failed. The work item is not found ,maybe ,it is claimed by others.");
		if (workItem.getState().getValue() != WorkItemState.INITIALIZED
				.getValue()) {

			throw new InvalidOperationException(
					"Claim work item failed. The state of the work item is "
							+ workItem.getState().getValue() + "("
							+ workItem.getState().getDisplayName() + ")");
		}

		ActivityInstance activityInstance = (ActivityInstance) workItem
				.getActivityInstance();

		if (activityInstance.getState().getValue() != ActivityInstanceState.INITIALIZED
				.getValue()
				&& activityInstance.getState().getValue() != ActivityInstanceState.RUNNING
						.getValue()) {
			throw new InvalidOperationException(
					"Claim work item failed .The state of the correspond activity instance is "
							+ activityInstance.getState() + "("
							+ activityInstance.getState().getDisplayName()
							+ ")");
		}

		if (activityInstance.isSuspended()) {
			throw new InvalidOperationException(
					"Claim work item failed .The  correspond activity instance is suspended");
		}

		WorkItemManager workItemMgr = rtCtx.getEngineModule(
				WorkItemManager.class, this.processType);
		return workItemMgr.claimWorkItem(session, workItem);
	}

	public List<WorkItem> disclaimWorkItem(String workItemId)
			throws InvalidOperationException {
		RuntimeContext rtCtx = this.session.getRuntimeContext();

		PersistenceService persistenceService = rtCtx.getEngineModule(
				PersistenceService.class, processType);
		WorkItemPersister workItemPersister = persistenceService
				.getWorkItemPersister();

		WorkItem workItem = workItemPersister.find(WorkItem.class, workItemId);

		if (workItem == null)
			throw new InvalidOperationException(
					"Disclaim work item failed. The work item is not found .");

		ActivityInstance activityInstance = (ActivityInstance) workItem
				.getActivityInstance();
		if (workItem.getState().getValue() != WorkItemState.RUNNING.getValue()) {

			throw new InvalidOperationException(
					"Disclaim work item failed. The state of the work item is "
							+ workItem.getState().getValue() + "("
							+ workItem.getState().getDisplayName() + ")");
		}
		if (activityInstance.getState().getValue() != ActivityInstanceState.INITIALIZED
				.getValue()
				&& activityInstance.getState().getValue() != ActivityInstanceState.RUNNING
						.getValue()) {
			throw new InvalidOperationException(
					"Dislaim work item failed .The state of the correspond activity instance is "
							+ activityInstance.getState() + "("
							+ activityInstance.getState().getDisplayName()
							+ ")");
		}

		if (activityInstance.isSuspended()) {
			throw new InvalidOperationException(
					"Disclaim work item failed .The  correspond activity instance is suspended");
		}

		WorkItemManager workItemMgr = rtCtx.getEngineModule(
				WorkItemManager.class, this.processType);
		return workItemMgr.disclaimWorkItem(session, workItem);
	}

	public WorkItem withdrawWorkItem(String workItemId)
			throws InvalidOperationException {
		RuntimeContext rtCtx = this.session.getRuntimeContext();

		PersistenceService persistenceService = rtCtx.getEngineModule(
				PersistenceService.class, processType);
		WorkItemPersister workItemPersister = persistenceService
				.getWorkItemPersister();

		WorkItem workItem = workItemPersister.find(WorkItem.class, workItemId);

		if (workItem == null)
			throw new InvalidOperationException(
					"Withdraw work item failed. The work item is not found.");

		if (workItem.getState().getValue() < WorkItemState.DELIMITER.getValue()) {

			throw new InvalidOperationException(
					"Withdraw work item failed. The state of the work item is "
							+ workItem.getState().getValue() + "("
							+ workItem.getState().getDisplayName() + ")");
		}

		WorkItemManager workItemMgr = rtCtx.getEngineModule(
				WorkItemManager.class, this.processType);
		return workItemMgr.withdrawWorkItem(session, workItem);
	}

	public void completeWorkItem(String workItemId)
			throws InvalidOperationException {
		RuntimeContext rtCtx = this.session.getRuntimeContext();

		PersistenceService persistenceService = rtCtx.getEngineModule(
				PersistenceService.class, processType);
		WorkItemPersister workItemPersister = persistenceService
				.getWorkItemPersister();

		WorkItem workItem = workItemPersister.find(WorkItem.class, workItemId);

		if (workItem == null)
			throw new InvalidOperationException(
					"Complete work item failed. The work item is not found .");

		ActivityInstance activityInstance = (ActivityInstance) workItem
				.getActivityInstance();
		if (workItem.getState().getValue() != WorkItemState.RUNNING.getValue()) {

			throw new InvalidOperationException(
					"Complete work item failed. The state of the work item is "
							+ workItem.getState().getValue() + "("
							+ workItem.getState().getDisplayName() + ")");
		}
		if (activityInstance.getState().getValue() != ActivityInstanceState.INITIALIZED
				.getValue()
				&& activityInstance.getState().getValue() != ActivityInstanceState.RUNNING
						.getValue()) {
			throw new InvalidOperationException(
					"Complete work item failed .The state of the correspond activity instance is "
							+ activityInstance.getState() + "("
							+ activityInstance.getState().getDisplayName()
							+ ")");
		}

		if (activityInstance.isSuspended()) {
			throw new InvalidOperationException(
					"Complete work item failed .The  correspond activity instance is suspended");
		}

		WorkItemManager workItemMgr = rtCtx.getEngineModule(
				WorkItemManager.class, this.processType);
		workItemMgr.completeWorkItem(session, workItem);
	}

	public void completeWorkItemAndJumpTo(String workItemId,
			String targetActivityId) throws InvalidOperationException {
		RuntimeContext rtCtx = this.session.getRuntimeContext();

		PersistenceService persistenceService = rtCtx.getEngineModule(
				PersistenceService.class, processType);
		WorkItemPersister workItemPersister = persistenceService
				.getWorkItemPersister();

		WorkItem workItem = workItemPersister.find(WorkItem.class, workItemId);

		if (workItem == null)
			throw new InvalidOperationException(
					"Complete work item failed. The work item is not found .");

		ActivityInstance activityInstance = (ActivityInstance) workItem
				.getActivityInstance();
		if (workItem.getState().getValue() != WorkItemState.RUNNING.getValue()) {

			throw new InvalidOperationException(
					"Complete work item failed. The state of the work item is "
							+ workItem.getState().getValue() + "("
							+ workItem.getState().getDisplayName() + ")");
		}
		if (activityInstance.getState().getValue() != ActivityInstanceState.INITIALIZED
				.getValue()
				&& activityInstance.getState().getValue() != ActivityInstanceState.RUNNING
						.getValue()) {
			throw new InvalidOperationException(
					"Complete work item failed .The state of the correspond activity instance is "
							+ activityInstance.getState() + "("
							+ activityInstance.getState().getDisplayName()
							+ ")");
		}

		if (activityInstance.isSuspended()) {
			throw new InvalidOperationException(
					"Complete work item failed .The  correspond activity instance is suspended");
		}

		WorkItemManager workItemMgr = rtCtx.getEngineModule(
				WorkItemManager.class, this.processType);
		workItemMgr.completeWorkItemAndJumpTo(session, workItem,
				targetActivityId);
	}

	public List<WorkItem> reassignWorkItemTo(String workItemId,
			List<User> users, String reassignType,
			WorkItemAssignmentStrategy assignmentStrategy)
			throws InvalidOperationException {
		RuntimeContext rtCtx = this.session.getRuntimeContext();

		PersistenceService persistenceService = rtCtx.getEngineModule(
				PersistenceService.class, processType);
		WorkItemPersister workItemPersister = persistenceService
				.getWorkItemPersister();

		WorkItem workItem = workItemPersister.find(WorkItem.class, workItemId);

		if (workItem == null)
			throw new InvalidOperationException(
					"Complete work item failed. The work item is not found .");

		ActivityInstance activityInstance = (ActivityInstance) workItem
				.getActivityInstance();
		if (workItem.getState().getValue() != WorkItemState.RUNNING.getValue()) {

			throw new InvalidOperationException(
					"Complete work item failed. The state of the work item is "
							+ workItem.getState().getValue() + "("
							+ workItem.getState().getDisplayName() + ")");
		}
		if (activityInstance.getState().getValue() != ActivityInstanceState.INITIALIZED
				.getValue()
				&& activityInstance.getState().getValue() != ActivityInstanceState.RUNNING
						.getValue()) {
			throw new InvalidOperationException(
					"Complete work item failed .The state of the correspond activity instance is "
							+ activityInstance.getState() + "("
							+ activityInstance.getState().getDisplayName()
							+ ")");
		}

		if (activityInstance.isSuspended()) {
			throw new InvalidOperationException(
					"Complete work item failed .The  correspond activity instance is suspended");
		}

		WorkItemManager workItemMgr = rtCtx.getEngineModule(
				WorkItemManager.class, this.processType);
		ProcessUtil processUtil = rtCtx.getEngineModule(ProcessUtil.class, this.processType);
		ProcessKey pKey = new ProcessKey(activityInstance.getProcessId(),activityInstance.getVersion(),activityInstance.getProcessType());

		Object theActivity = null;
		try {
			theActivity = processUtil.getActivity(pKey, activityInstance.getSubflowId(), activityInstance.getNodeId());
		} catch (InvalidModelException e) {
			throw new InvalidOperationException(e);
		}
		return workItemMgr.reassignWorkItemTo(session, workItem, users,
				reassignType, assignmentStrategy,theActivity);
	}

	/*****************************************************************/
	/*************** 流程定义相关的api ******************************/
	/*****************************************************************/
	public ProcessRepository uploadProcess(Object process,
			boolean publishState, Map<ProcessDescriptorProperty, Object> metadata)
			throws InvalidModelException {
		RuntimeContext ctx = this.session.getRuntimeContext();
		PersistenceService persistenceService = ctx.getEngineModule(
				PersistenceService.class, processType);
		ProcessPersister processPersister = persistenceService
				.getProcessPersister();
		CalendarService calendarService = ctx.getEngineModule(
				CalendarService.class, this.processType);

		HashMap<ProcessDescriptorProperty, Object> props = new HashMap<ProcessDescriptorProperty, Object>();
		if (metadata != null){
			props.putAll(metadata);
		}			

		props.put(ProcessDescriptorProperty.PUBLISH_STATE, publishState);
		props.put(ProcessDescriptorProperty.PROCESS_TYPE, processType);
		//TODO LATEST_EDIT_TIME应该让数据库系统自动生成？
		props.put(ProcessDescriptorProperty.LATEST_EDIT_TIME,
				calendarService.getSysDate());
		props.put(ProcessDescriptorProperty.LATEST_EDITOR, this.session
				.getCurrentUser().getId()
				+ "["
				+ this.session.getCurrentUser().getName() + "]");// 直接用Id+Name增强可读性

		return processPersister.persistProcessToRepository(process, props);
	}

	/*****************************************************************/
	/*************** 流程变量相关的api ******************************/
	/*****************************************************************/

	public Object getVariableValue(Scope scope, String name) {
		RuntimeContext ctx = this.session.getRuntimeContext();
		PersistenceService persistenceService = ctx.getEngineModule(
				PersistenceService.class, this.processType);
		VariablePersister variablePersister = persistenceService
				.getVariablePersister();
		
		Variable var = variablePersister.findVariable(scope.getScopeId(), name);
		if (var != null) {
			return var.getPayload();
		} else {
			return null;
		}

	}
	
	public void setVariableValue(Scope scope, String name, Object value)
	throws InvalidOperationException {
		this.setVariableValue(scope, name, value,null);
	}

	public void setVariableValue(Scope scope, String name, Object value,Map<String,String> headers)
			throws InvalidOperationException {
		RuntimeContext ctx = this.session.getRuntimeContext();

		// 进行流程变量的类型校验，如果流程定义中有名称为参数“name”的变量定义，则value的类型必须和变量的类型相匹配。
		ProcessUtil processUtil = ctx.getEngineModule(ProcessUtil.class,
				scope.getProcessType());
		
		Property property = null;
		try{
			property = processUtil.getProperty(
					new ProcessKey(scope.getProcessId(), scope.getVersion(), scope
							.getProcessType()), scope.getProcessElementId(), name);
		}catch(InvalidModelException e){
			throw new InvalidOperationException(e);
		}


		if (property != null && property.getDataType() != null && value != null) {
			QName qName = property.getDataType();
			// java类型
			if (qName.getNamespaceURI().endsWith(NameSpaces.JAVA.getUri())) {
				String className = qName.getLocalPart();

				if (value instanceof org.w3c.dom.Document
						|| value instanceof org.dom4j.Document) {
					throw new ClassCastException(
							"Can NOT cast from xml content to " + className);
				}

				try {
					Class clz = Class.forName(className);
					if (!clz.isInstance(value)) {
						throw new ClassCastException("Can NOT cast from "
								+ value.getClass().getName() + " to "
								+ className);
					}
				} catch (ClassNotFoundException e) {
					throw new InvalidOperationException(e);
				}
			} 
			// xml类型
			else {
				if (!(value instanceof org.w3c.dom.Document)
						&& !(value instanceof org.dom4j.Document)) {
					throw new ClassCastException(
							"Can NOT cast from "+value.getClass().getName()+" to " +qName );
				}

			}
		}
		PersistenceService persistenceService = ctx.getEngineModule(
				PersistenceService.class, this.processType);
		VariablePersister variablePersister = persistenceService
				.getVariablePersister();
		Variable v = variablePersister.findVariable(scope.getScopeId(), name);
		if (v!=null){
			((AbsVariable)v).setPayload(value);
			if (headers!=null && headers.size()>0){
				v.getHeaders().putAll(headers);
			}
			variablePersister.saveOrUpdate(v);
		}else{
			v = new VariableImpl();
			((AbsVariable)v).setScopeId(scope.getScopeId());
			((AbsVariable)v).setName(name);
			((AbsVariable)v).setProcessElementId(scope.getProcessElementId());
			((AbsVariable)v).setPayload(value);
			if (value!=null){
				if (value instanceof org.w3c.dom.Document){
					if (property != null && property.getDataType() != null ){
						((AbsVariable)v).setDataType(property.getDataType());
					}
					v.getHeaders().put(Variable.HEADER_KEY_CLASS_NAME, "org.w3c.dom.Document");
				}else if (value instanceof org.dom4j.Document){
					if (property != null && property.getDataType() != null ){
						((AbsVariable)v).setDataType(property.getDataType());
					}
					v.getHeaders().put(Variable.HEADER_KEY_CLASS_NAME, "org.dom4j.Document");
				}else{
					((AbsVariable)v).setDataType(new QName(NameSpaces.JAVA.getUri(),value.getClass().getName()));
				}
				
			}
			((AbsVariable)v).setProcessId(scope.getProcessId());
			((AbsVariable)v).setVersion(scope.getVersion());
			((AbsVariable)v).setProcessType(scope.getProcessType());
			
			if (headers!=null && headers.size()>0){
				v.getHeaders().putAll(headers);
			}
			variablePersister.saveOrUpdate(v);
		}
		return;
	}

	public Map<String, Object> getVariableValues(Scope scope) {
		RuntimeContext ctx = this.session.getRuntimeContext();
		PersistenceService persistenceService = ctx.getEngineModule(
				PersistenceService.class, this.processType);
		VariablePersister variablePersister = persistenceService
				.getVariablePersister();
		
		List<Variable> vars = variablePersister.findVariables(scope.getScopeId());
		Map<String,Object> varValues = new HashMap<String,Object>();
		if (vars!=null && vars.size()>0){
			for (Variable var : vars){
				varValues.put(var.getName(), var.getPayload());
			}
		}
		return varValues;
	}

	public <T extends WorkflowEntity> List<T> executeQueryList(
			WorkflowQuery<T> q) {
		Class<T> entityClass = q.getEntityClass();
		Persister persister = this.getPersister(entityClass);
		return persister.list(q);
	}

	public <T extends WorkflowEntity> int executeQueryCount(WorkflowQuery<T> q) {
		Class<T> entityClass = q.getEntityClass();
		Persister persister = this.getPersister(entityClass);
		return persister.count(q);
	}

	public <T extends WorkflowEntity> T getEntity(String entityId,
			Class<T> entityClass) {
		Persister persister = this.getPersister(entityClass);
		return persister.find(entityClass, entityId);
	}

	public Object getWorkflowProcess(ProcessKey key)
			throws InvalidModelException {
		RuntimeContext runtimeContext = this.session.getRuntimeContext();
		PersistenceService persistenceService = runtimeContext.getEngineModule(
				PersistenceService.class, this.processType);
		ProcessPersister processPersister = persistenceService
				.getProcessPersister();
		ProcessRepository repository = processPersister
				.findProcessRepositoryByProcessKey(key);

		return repository.getProcess();
	}

	private <T> Persister getPersister(Class<T> entityClass) {
		RuntimeContext runtimeContext = this.session.getRuntimeContext();
		PersistenceService persistenceService = runtimeContext.getEngineModule(
				PersistenceService.class, this.processType);
		Persister persister = null;
		if (entityClass.isAssignableFrom(ActivityInstance.class)) {
			persister = persistenceService.getActivityInstancePersister();
		} else if (entityClass.isAssignableFrom(ProcessInstance.class)) {
			persister = persistenceService.getProcessInstancePersister();
		} else if (entityClass.isAssignableFrom(WorkItem.class)) {
			persister = persistenceService.getWorkItemPersister();
		} else if (entityClass.isAssignableFrom(Token.class)) {
			persister = persistenceService.getTokenPersister();
		} else if (entityClass.isAssignableFrom(Variable.class)) {
			persister = persistenceService.getVariablePersister();
		} else if (entityClass.isAssignableFrom(ProcessRepository.class)
				|| entityClass.isAssignableFrom(ProcessDescriptor.class)) {
			persister = persistenceService.getProcessPersister();
		} else if (entityClass.isAssignableFrom(ScheduleJob.class)) {
			persister = persistenceService.getScheduleJobPersister();
		}
		return persister;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.WorkflowStatement#uploadResources(java.io.InputStream
	 * , java.util.Map)
	 */
	public ResourceRepository uploadResources(InputStream inStream,
			Map<ResourceDescriptorProperty, Object> resourceDescriptorKeyValue)
			throws InvalidModelException {
		Map<ResourceDescriptorProperty, Object> props = new HashMap<ResourceDescriptorProperty, Object>();

		if (resourceDescriptorKeyValue != null) {
			props.putAll(resourceDescriptorKeyValue);
		}
		RuntimeContext ctx = this.session.getRuntimeContext();
		PersistenceService persistenceService = ctx.getEngineModule(
				PersistenceService.class, this.processType);
		ResourcePersister resourcePersister = persistenceService
				.getResourcePersister();
		CalendarService calendarService = ctx.getEngineModule(
				CalendarService.class, this.processType);
		props.put(ResourceDescriptorProperty.LATEST_EDITOR, this.session
				.getCurrentUser().getId()
				+ "["
				+ this.session.getCurrentUser().getName() + "]");
		props.put(ResourceDescriptorProperty.LATEST_EDIT_TIME,
				calendarService.getSysDate());

		return resourcePersister.persistResourceFileToRepository(inStream,
				props);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fireflow.engine.WorkflowStatement#uploadServices(java.io.InputStream,
	 * java.util.Map)
	 */
	public ServiceRepository uploadServices(InputStream inStream,
			Map<ServiceDescriptorProperty, Object> serviceDescriptorKeyValue)
			throws InvalidModelException {
		Map<ServiceDescriptorProperty, Object> props = new HashMap<ServiceDescriptorProperty, Object>();

		if (serviceDescriptorKeyValue != null) {
			props.putAll(serviceDescriptorKeyValue);
		}
		RuntimeContext ctx = this.session.getRuntimeContext();
		PersistenceService persistenceService = ctx.getEngineModule(
				PersistenceService.class, this.processType);
		ServicePersister servicePersister = persistenceService
				.getServicePersister();
		CalendarService calendarService = ctx.getEngineModule(
				CalendarService.class, this.processType);
		props.put(ServiceDescriptorProperty.LATEST_EDITOR, this.session
				.getCurrentUser().getId()
				+ "["
				+ this.session.getCurrentUser().getName() + "]");
		props.put(ServiceDescriptorProperty.LATEST_EDIT_TIME,
				calendarService.getSysDate());

		return servicePersister.persistServiceFileToRepository(inStream, props);
	}

	/**
	 * WorkflowStatement都是客户端调用，在调用执行前，把上一次调用的一些遗留清除
	 * @param session
	 */
	private void resetSession(WorkflowSessionLocalImpl session){
		session.setCurrentActivityInstance(null);
		session.setCurrentProcessInstance(null);
		session.setLatestCreatedWorkItems(null);
	}
}
