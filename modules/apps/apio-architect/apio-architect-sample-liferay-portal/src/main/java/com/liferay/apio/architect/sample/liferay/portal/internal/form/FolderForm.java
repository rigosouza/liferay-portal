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

package com.liferay.apio.architect.sample.liferay.portal.internal.form;

import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.form.Form.Builder;

/**
 * Represents the values extracted from a folder form.
 *
 * @author Alejandro Hernández
 */
public class FolderForm {

	/**
	 * Builds a {@code Form} that generates {@code FolderForm} that depends on
	 * the HTTP body.
	 *
	 * @param  formBuilder the form builder
	 * @return the folder form
	 */
	public static Form<FolderForm> buildForm(Builder<FolderForm> formBuilder) {
		return formBuilder.title(
			__ -> "The folder form"
		).description(
			__ -> "This form can be used to create or update a folder"
		).constructor(
			FolderForm::new
		).addRequiredString(
			"description", FolderForm::_setDescription
		).addRequiredString(
			"name", FolderForm::_setName
		).build();
	}

	/**
	 * Returns the folder's description.
	 *
	 * @return the folder's description
	 */
	public String getDescription() {
		return _description;
	}

	/**
	 * Returns the folder's name.
	 *
	 * @return the folder's name
	 */
	public String getName() {
		return _name;
	}

	private void _setDescription(String description) {
		_description = description;
	}

	private void _setName(String name) {
		_name = name;
	}

	private String _description;
	private String _name;

}