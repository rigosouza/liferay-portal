/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.user.associated.data.aggregator;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.user.associated.data.entity.UADEntity;

import java.util.List;

/**
 * @author William Newbury
 */
public abstract class BaseUADEntityAggregator implements UADEntityAggregator {

	@Override
	public long count(long userId) {
		List<UADEntity> userIdUADEntities = getUADEntities(userId);

		return userIdUADEntities.size();
	}

	@Override
	public abstract List<UADEntity> getUADEntities(long userId);

	@Override
	public abstract UADEntity getUADEntity(String uadEntityId)
		throws PortalException;

}