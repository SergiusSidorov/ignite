/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.marshaller;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.util.IgniteUtils;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility marshaller methods.
 */
public class MarshallerUtils {
    /**
     * Marshal object with node name taken from provided kernal context.
     *
     * @param ctx Kernal context.
     * @param obj Object to marshal.
     * @return Result.
     * @throws IgniteCheckedException If failed.
     */
    public static byte[] marshal(GridKernalContext ctx, @Nullable Object obj) throws IgniteCheckedException {
        return ctx.config().getMarshaller().marshal(obj);
    }

    /**
     * Unmarshal object and set grid name thread local.
     *
     * @param name Grid name.
     * @param marsh Marshaller.
     * @param arr Binary data.
     * @param ldr Class loader.
     * @return Deserialized object.
     * @throws IgniteCheckedException If failed.
     */
    public static <T> T unmarshal(String name, Marshaller marsh, byte[] arr, @Nullable ClassLoader ldr)
        throws IgniteCheckedException {
        String oldName = IgniteUtils.setCurrentIgniteName(name);

        try {
            return marsh.unmarshal(arr, ldr);
        }
        finally {
            IgniteUtils.restoreCurrentIgniteName(oldName);
        }
    }

    /**
     * Unmarshal object from stream and set grid name thread local.
     *
     * @param name Grid name.
     * @param marsh Marshaller.
     * @param in Input stream.
     * @param ldr Class loader.
     * @return Deserialized object.
     * @throws IgniteCheckedException If failed.
     */
    public static <T> T unmarshal(String name, Marshaller marsh, InputStream in, @Nullable ClassLoader ldr)
        throws IgniteCheckedException {
        String oldName = IgniteUtils.setCurrentIgniteName(name);

        try {
            return marsh.unmarshal(in, ldr);
        }
        finally {
            IgniteUtils.restoreCurrentIgniteName(oldName);
        }
    }

    /**
     * Marshal and unmarshal object.
     *
     * @param name Grid name.
     * @param marsh Marshaller.
     * @param obj Object to clone.
     * @param clsLdr Class loader.
     * @return Deserialized value.
     * @throws IgniteCheckedException If failed.
     */
    public static <T> T marshalUnmarshal(String name, Marshaller marsh, T obj, @Nullable ClassLoader clsLdr)
        throws IgniteCheckedException {
        String oldName = IgniteUtils.setCurrentIgniteName(name);

        try {
            return marsh.unmarshal(marsh.marshal(obj), clsLdr);
        }
        finally {
            IgniteUtils.restoreCurrentIgniteName(oldName);
        }
    }

    /**
     * Private constructor.
     */
    private MarshallerUtils() {
        // No-op.
    }
}
