/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.config.service;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.opennms.core.utils.ConfigFileConstants;
import org.opennms.core.xml.JaxbUtils;
import org.springframework.stereotype.Service;


/**
 * We maintain and serve configurations from a centralized place in a uniform way.
 */
@Service
public class ConfigurationService {

    /**
     * Loads the latest available configuration specified by the URI and transforms it into the given JaxB class.
     */
    public <T> T getConfigurationAsJaxb(final String uri, final Class<T> clazz) throws ConfigurationNotAvailableException {
        Objects.requireNonNull(uri, "Uri cannot be null");
        Objects.requireNonNull(clazz, "Jaxb class cannot be null");
        String config = getConfigurationAsString(uri);
        return JaxbUtils.unmarshal(clazz, new StringReader(config));
    }

    /**
     * Loads the latest available configuration specified by the URI as a raw String. It makes no assumptions about it's
     * format, e.g. XML, JSON or Properties
     */
    public String getConfigurationAsString(final String uri) throws ConfigurationNotAvailableException {
        try {
            // for now we only support files in the etc dir:
            File file = ConfigFileConstants.getConfigFileByName(uri);
            return FileUtils.readFileToString(file, Charset.defaultCharset());
        } catch(IOException e) {
            throw new ConfigurationNotAvailableException(e);
        }
    }
}
