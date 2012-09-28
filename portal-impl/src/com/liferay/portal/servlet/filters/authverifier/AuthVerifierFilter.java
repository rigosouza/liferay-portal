/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
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

package com.liferay.portal.servlet.filters.authverifier;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ProtectedServletRequest;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.ac.AccessControlUtil;
import com.liferay.portal.security.auth.AccessControlContext;
import com.liferay.portal.security.auth.AuthSettingsUtil;
import com.liferay.portal.security.auth.AuthVerifierPipeline;
import com.liferay.portal.security.auth.AuthVerifierResult;
import com.liferay.portal.servlet.filters.BasePortalFilter;
import com.liferay.portal.util.PropsUtil;

import java.io.IOException;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * See http://issues.liferay.com/browse/LPS-27888.
 * </p>
 *
 * @author Tomas Polesovsky
 * @author Raymond Augé
 */
public class AuthVerifierFilter extends BasePortalFilter {

	@Override
	public void init(FilterConfig filterConfig) {
		super.init(filterConfig);

		Enumeration<String> filterInitParameters =
			filterConfig.getInitParameterNames();

		while (filterInitParameters.hasMoreElements()) {
			String name = filterInitParameters.nextElement();
			String value = filterConfig.getInitParameter(name);

			_filterConfiguration.put(name, value);
		}

		String propertyPrefix =
			GetterUtil.getString(
				_filterConfiguration.get("portal_property_prefix"));

		if (Validator.isNotNull(propertyPrefix)) {
			Properties filterPortalConfiguration = PropsUtil.getProperties(
				propertyPrefix, true);

			for (Object name : filterPortalConfiguration.keySet()) {
				Object value = filterPortalConfiguration.get(name);
				_filterConfiguration.put((String) name, value);
			}
		}

		if (_filterConfiguration.containsKey("https.required")) {
			_httpsRequired =
				GetterUtil.getBoolean(
					_filterConfiguration.get("https.required"));

			_filterConfiguration.remove("https.required");
		}

		if (_filterConfiguration.containsKey("hosts.allowed")) {
			_hostsAllowed.addAll(ListUtil.fromArray(StringUtil.split(
				(String) _filterConfiguration.get("hosts.allowed"))));

			_filterConfiguration.remove("hosts.allowed");
		}

		if (_filterConfiguration.containsKey("use_permission_checker")) {
			_filterConfiguration.remove("use_permission_checker");
			_log.warn(
				"use_permission_checker was deprecated and has no effect!");
		}

	}

	@Override
	protected void processFilter(
			HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain)
		throws Exception {

		if (!_accessAllowed(request, response)) {
			return;
		}

		if (_sslApplied(request, response)) {
			return;
		}

		AccessControlUtil.initAccessControlContext(
			request, response, _filterConfiguration);

		AuthVerifierResult.State state = AccessControlUtil.verifyRequest();

		AccessControlContext accessControlContext =
			AccessControlUtil.getAccessControlContext();

		AuthVerifierResult authVerifierResult =
			accessControlContext.getAuthVerifierResult();

		if (_log.isDebugEnabled()) {
			_log.debug("Auth verifier result " + authVerifierResult);
		}

		if (state == AuthVerifierResult.State.INVALID_CREDENTIALS) {
			if (_log.isDebugEnabled()) {
				_log.debug("Result state doesn't allow us to continue.");
			}
		}
		else if (state == AuthVerifierResult.State.NOT_APPLICABLE) {
			_log.error("Invalid state " + state);
		}
		else if (state == AuthVerifierResult.State.SUCCESS) {
			long userId = authVerifierResult.getUserId();

			AccessControlUtil.initContextUser(userId);

			Map<String, Object> settings = accessControlContext.getSettings();

			String authType = (String)settings.get(
				AuthVerifierPipeline.AUTH_TYPE);

			ProtectedServletRequest protectedServletRequest =
				new ProtectedServletRequest(
					request, String.valueOf(userId), authType);

			accessControlContext.setRequest(protectedServletRequest);

			processFilter(
				getClass(), protectedServletRequest, response, filterChain);
		}
		else {
			_log.error("Unimplemented state " + state);
		}
	}

	private boolean _accessAllowed(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String remoteAddr = request.getRemoteAddr();

		if (AuthSettingsUtil.isAccessAllowed(request, _hostsAllowed)) {
			if (_log.isDebugEnabled()) {
				_log.debug("Access allowed for " + remoteAddr);
			}

			return true;
		}

		if (_log.isWarnEnabled()) {
			_log.warn("Access denied for " + remoteAddr);
		}

		response.sendError(
			HttpServletResponse.SC_FORBIDDEN,
			"Access denied for " + remoteAddr);

		return false;
	}

	private boolean _sslApplied(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		if (!_httpsRequired || request.isSecure()) {
			return false;
		}

		if (_log.isDebugEnabled()) {
			String completeURL = HttpUtil.getCompleteURL(request);

			_log.debug("Securing " + completeURL);
		}

		StringBundler redirectURL = new StringBundler(5);

		redirectURL.append(Http.HTTPS_WITH_SLASH);
		redirectURL.append(request.getServerName());
		redirectURL.append(request.getServletPath());

		String queryString = request.getQueryString();

		if (Validator.isNotNull(queryString)) {
			redirectURL.append(StringPool.QUESTION);
			redirectURL.append(request.getQueryString());
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Redirect to " + redirectURL);
		}

		response.sendRedirect(redirectURL.toString());

		return true;
	}

	private static Log _log = LogFactoryUtil.getLog(
		AuthVerifierFilter.class.getName());

	private Map<String, Object> _filterConfiguration =
		new HashMap<String, Object>();
	private Set<String> _hostsAllowed = new HashSet<String>();

	private boolean _httpsRequired;

}