/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.service.permission;

import com.google.common.base.Optional;
import org.spongepowered.api.service.permission.MemorySubjectData;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.command.CommandSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpLevelSubject extends SpongeSubject {
    private final int level;
    private final MemorySubjectData data;
    public OpLevelSubject(OpPermissionService service, int level) {
        super(service);
        this.level = level;
        this.data = new OpLevelSubjectData(service);
    }

    @Override
    public String getIdentifier() {
        return "op_" + level;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.absent();
    }

    @Override
    public SubjectCollection getContainingCollection() {
        return getService().getGroupSubjects();
    }

    @Override
    public SubjectData getData() {
        return data;
    }

    /**
     * Variant of {@link MemorySubjectData} that gets parents based on the server's op setting for a given user
     */
    private class OpLevelSubjectData extends MemorySubjectData {

        public OpLevelSubjectData(PermissionService service) {
            super(service);
        }

        @Override
        public Map<Set<Context>, List<Subject>> getAllParents() {
            return super.getAllParents();
        }

        @Override
        public List<Subject> getParents(Set<Context> contexts) {
            return super.getParents(contexts);
        }

        @Override
        public boolean addParent(Set<Context> contexts, Subject parent) {
            if (!(parent instanceof OpLevelSubject)) {
                return false;
            }
            return super.addParent(contexts, parent);
        }

        @Override
        public boolean removeParent(Set<Context> contexts, Subject parent) {
            if (!(parent instanceof OpLevelSubject)) {
                return false;
            }
            return super.removeParent(contexts, parent);
        }

        @Override
        public boolean clearParents() {
            return super.clearParents();
        }

        @Override
        public boolean clearParents(Set<Context> contexts) {
            return super.clearParents(contexts);
        }
    }
}
