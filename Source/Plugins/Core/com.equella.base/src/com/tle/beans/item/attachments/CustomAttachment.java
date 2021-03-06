/*
 * Copyright 2017 Apereo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tle.beans.item.attachments;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.AccessType;

@Entity
@AccessType("field")
@DiscriminatorValue("custom")
public class CustomAttachment extends Attachment
{
	private static final long serialVersionUID = 1L;

	public CustomAttachment()
	{
		super();
	}

	public void setType(String type)
	{
		value1 = type;
	}

	public String getType()
	{
		return value1;
	}

	@Override
	public AttachmentType getAttachmentType()
	{
		return AttachmentType.CUSTOM;
	}
}
