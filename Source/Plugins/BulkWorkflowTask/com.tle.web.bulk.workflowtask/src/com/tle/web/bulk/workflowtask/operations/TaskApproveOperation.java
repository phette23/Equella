package com.tle.web.bulk.workflowtask.operations;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Throwables;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.tle.beans.item.HistoryEvent;
import com.tle.beans.item.HistoryEvent.Type;
import com.tle.beans.item.ItemTaskId;
import com.tle.common.Check;
import com.tle.common.filesystem.handle.StagingFile;
import com.tle.common.usermanagement.user.CurrentUser;
import com.tle.common.workflow.WorkflowItemStatus;
import com.tle.common.workflow.WorkflowMessage;
import com.tle.common.workflow.node.WorkflowItem;
import com.tle.core.filesystem.WorkflowMessageFile;
import com.tle.core.item.standard.workflow.nodes.TaskStatus;

public class TaskApproveOperation extends AbstractBulkTaskOperation
{
	private final String message;
	private final boolean acceptAllUsers;

	@AssistedInject
	public TaskApproveOperation(@Assisted("message") String message,
		@Assisted("acceptAllUsers") boolean acceptAllUsers)
	{
		this.message = message;
		this.acceptAllUsers = acceptAllUsers;
	}

	@Override
	public boolean execute()
	{
		TaskStatus status = init();
		ItemTaskId taskId = getTaskId();
		WorkflowItemStatus bean = (WorkflowItemStatus) status.getBean();
		WorkflowItem workflowItem = (WorkflowItem) status.getWorkflowNode();

		final Set<String> acceptedUsers = bean.getAcceptedUsers();
		if( acceptAllUsers )
		{
			// EQ-2544 a bulk approval/reject from ManageTask page should move
			// the item onward, even when Unanimous=True.
			acceptedUsers.addAll(getUsersToModerate(workflowItem));
		}
		else
		{
			acceptedUsers.add(CurrentUser.getUserID());
		}

		params.setCause(status.getBean());
		getModerationStatus().setLastAction(params.getDateNow());
		HistoryEvent approved = createHistory(Type.approved);
		setStepFromTask(approved, taskId.getTaskId());

		if( !Check.isEmpty(message) )
		{
			approved.setComment(message);
			addMessage(taskId.getTaskId(), WorkflowMessage.TYPE_ACCEPT, message, null);
		}

		status.update();
		updateModeration();
		return true;
	}
}
