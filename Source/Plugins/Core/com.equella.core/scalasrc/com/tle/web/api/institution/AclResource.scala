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

package com.tle.web.api.institution

import com.tle.common.security.PrivilegeTree.Node
import com.tle.common.security.{PrivilegeTree, TargetList, TargetListEntry}
import com.tle.core.security.AclPrefs
import com.tle.core.settings.UserPrefs
import com.tle.exceptions.PrivilegeRequiredException
import com.tle.legacy.LegacyGuice
import com.tle.web.api.interfaces.beans.security.{TargetListBean, TargetListEntryBean}
import io.circe.Decoder
import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import javax.ws.rs._
import sbt.io.Path

import scala.collection.JavaConverters._

@Produces(value =Array("application/json"))
@Path("acl/")
@Api(value = "ACLs")
class AclResource {

	val aclManager = LegacyGuice.aclManager
	val userPrefs = LegacyGuice.userPreferenceService


	@GET
	@ApiOperation(value = "Get allowed privileges for tree node")
	@Path("/privileges") def getAllowedPrivileges(@QueryParam("node") node: Node) = {
		PrivilegeTree.getAllPrivilegesForNode(node).keySet().asScala.toVector.sorted
	}

	@GET
	@ApiOperation(value = "Determine if you have non-entity specific privilege(s)")
	@Path("/privilegecheck") def checkPrivilege(@QueryParam("privilege") privs: Array[String]) : Iterable[String]  = {
		aclManager.filterNonGrantedPrivileges(privs: _*).asScala
	}

	@GET
	@ApiOperation(value = "Get all institution level acls")
	@Path("/")
	def getEntries: TargetListBean = {
		checkPrivs("VIEW_SECURITY_TREE", "EDIT_SECURITY_TREE")
		val targetListBean = new TargetListBean
		val allAcls = aclManager.getTargetList(Node.INSTITUTION, null)
		val tBeanList = allAcls.getEntries.asScala.map { ae =>
			val tBean = new TargetListEntryBean
			tBean.setGranted(ae.isGranted)
			tBean.setOverride(ae.isOverride)
			tBean.setPrivilege(ae.getPrivilege)
			tBean.setWho(ae.getWho)
			tBean
		}
		targetListBean.setEntries(tBeanList.asJava)
		targetListBean
	}

	def checkPrivs(privs: String*): Unit = {
		if (aclManager.filterNonGrantedPrivileges(privs: _*).isEmpty) throw new PrivilegeRequiredException(privs: _*)
	}

	@PUT
	@ApiOperation(value = "Set all institution level acls")
	@Path("/")
	def setEntries(@ApiParam bean: TargetListBean): Response = {
		checkPrivs("EDIT_SECURITY_TREE")
		val tle = bean.getEntries.asScala.map { eb =>
			new TargetListEntry(eb.isGranted, eb.isOverride, eb.getPrivilege, eb.getWho)
		}
		aclManager.setTargetList(Node.INSTITUTION, null, new TargetList(tle.asJava))
		Response.status(Status.OK).build
	}

	@GET
	@ApiOperation(value = "Get recently used expression targets")
	@Path("/recent") def getRecent : Iterable[String] = {
		AclPrefs.getRecentTargets
	}

	@POST
	@ApiOperation(value = "Add an expression target to the recently used")
	@Path("/recent/add")
	def addRecent(@QueryParam("target") target : String) : Unit = {
		AclPrefs.addRecent(target)
	}

}
