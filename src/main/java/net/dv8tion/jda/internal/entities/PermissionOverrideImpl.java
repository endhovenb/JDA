/*
 * Copyright 2015-2019 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.internal.entities;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.requests.restaction.PermissionOverrideActionImpl;
import net.dv8tion.jda.internal.utils.cache.UpstreamReference;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantLock;

public class PermissionOverrideImpl implements PermissionOverride
{
    private final long id;
    private final long channelId;
    private final long holderId;
    private final ChannelType channelType;
    private final boolean role;
    private final UpstreamReference<JDAImpl> api;

    protected final ReentrantLock mngLock = new ReentrantLock();
    protected volatile PermissionOverrideAction manager;

    private long allow;
    private long deny;

    public PermissionOverrideImpl(GuildChannel channel, long id, IPermissionHolder permissionHolder)
    {
        this.channelId = channel.getIdLong();
        this.holderId = permissionHolder.getIdLong();
        this.channelType = channel.getType();
        this.role = permissionHolder instanceof Role;
        this.id = id;
        this.api = new UpstreamReference<>((JDAImpl) channel.getJDA());
    }

    @Override
    public long getAllowedRaw()
    {
        return allow;
    }

    @Override
    public long getInheritRaw()
    {
        return ~(allow | deny);
    }

    @Override
    public long getDeniedRaw()
    {
        return deny;
    }

    @Nonnull
    @Override
    public EnumSet<Permission> getAllowed()
    {
        return Permission.getPermissions(allow);
    }

    @Nonnull
    @Override
    public EnumSet<Permission> getInherit()
    {
        return Permission.getPermissions(getInheritRaw());
    }

    @Nonnull
    @Override
    public EnumSet<Permission> getDenied()
    {
        return Permission.getPermissions(deny);
    }

    @Nonnull
    @Override
    public JDA getJDA()
    {
        return api.get();
    }

    @Override
    public Member getMember()
    {
        return getGuild().getMemberById(holderId);
    }

    @Override
    public Role getRole()
    {
        return getGuild().getRoleById(holderId);
    }

    @Nonnull
    @Override
    public GuildChannel getChannel()
    {
        JDAImpl jda = api.get();
        GuildChannel channel;
        switch (channelType)
        {
            case TEXT:
                channel = jda.getTextChannelById(channelId);
                break;
            case VOICE:
                channel = jda.getVoiceChannelById(channelId);
                break;
            case CATEGORY:
                channel = jda.getCategoryById(channelId);
                break;
                //TODO: STORE
            default:
                throw new IllegalStateException("Unknown channel type " + channelType);
        }
        if (channel == null)
            throw new IllegalStateException("Cannot get reference to upstream " + channelType.name() + " Channel with id: " + Long.toUnsignedString(channelId));
        return channel;
    }

    @Nonnull
    @Override
    public Guild getGuild()
    {
        return getChannel().getGuild();
    }

    @Override
    public boolean isMemberOverride()
    {
        return !role;
    }

    @Override
    public boolean isRoleOverride()
    {
        return role;
    }

    @Nonnull
    @Override
    public PermissionOverrideAction getManager()
    {
        if (!getGuild().getSelfMember().hasPermission(getChannel(), Permission.MANAGE_PERMISSIONS))
            throw new InsufficientPermissionException(Permission.MANAGE_PERMISSIONS);
        PermissionOverrideAction mng = manager;
        if (mng == null)
        {
            mng = MiscUtil.locked(mngLock, () ->
            {
                if (manager == null)
                    manager = new PermissionOverrideActionImpl(this).setOverride(false);
                return manager;
            });
        }
        return mng.reset();
    }

    @Nonnull
    @Override
    public AuditableRestAction<Void> delete()
    {
        if (!getGuild().getSelfMember().hasPermission(getChannel(), Permission.MANAGE_PERMISSIONS))
            throw new InsufficientPermissionException(Permission.MANAGE_PERMISSIONS);

        @SuppressWarnings("ConstantConditions")
        String targetId = isRoleOverride() ? getRole().getId() : getMember().getUser().getId();
        Route.CompiledRoute route = Route.Channels.DELETE_PERM_OVERRIDE.compile(getChannel().getId(), targetId);
        return new AuditableRestActionImpl<>(getJDA(), route);
    }

    public PermissionOverrideImpl setAllow(long allow)
    {
        this.allow = allow;
        return this;
    }

    public PermissionOverrideImpl setDeny(long deny)
    {
        this.deny = deny;
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof PermissionOverrideImpl))
            return false;
        PermissionOverrideImpl oPerm = (PermissionOverrideImpl) o;
        return this.holderId == oPerm.holderId && this.channelId == oPerm.channelId;
    }

    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    @Override
    public String toString()
    {
        return "PermOver:(" + (isMemberOverride() ? "M" : "R") + ")(" + getChannel().getId() + " | " + id + ")";
    }

}
