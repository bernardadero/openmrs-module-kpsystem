/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.datatype;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link LocationDatatype}
 */
public class LocationDatatypeTest extends BaseModuleContextSensitiveTest {

	private LocationDatatype datatype = new LocationDatatype();

	/**
	 * @see LocationDatatype#deserialize(String)
	 */
	@Test
	public void deserialize() {
		Assert.assertThat(datatype.deserialize(null), is(nullValue()));
		Assert.assertThat(datatype.deserialize(""), is(nullValue()));
		Assert.assertThat(datatype.deserialize("2"), is(Context.getLocationService().getLocation(2)));
	}

	/**
	 * @see LocationDatatype#serialize(org.openmrs.Location)
	 */
	@Test
	public void serialize() {
		Assert.assertNull(datatype.serialize(null));
		Assert.assertThat(datatype.serialize(null), is(nullValue()));
		Assert.assertThat(datatype.serialize(Context.getLocationService().getLocation(2)), is("2"));
	}
}