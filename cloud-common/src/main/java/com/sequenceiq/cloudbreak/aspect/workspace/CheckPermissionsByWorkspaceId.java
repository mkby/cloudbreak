package com.sequenceiq.cloudbreak.aspect.workspace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckPermissionsByWorkspaceId {

    Action action() default Action.READ;

    int workspaceIdIndex() default 0;
}
