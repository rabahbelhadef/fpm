package com.mappy.data.batches.tomtom.dbf.speedrestrictions;

import com.mappy.data.batches.tomtom.TomtomFolder;
import com.mappy.data.batches.tomtom.dbf.speedrestrictions.SpeedRestriction;
import com.mappy.data.batches.tomtom.dbf.speedrestrictions.SrDbf;

import org.junit.Test;

import static com.mappy.data.batches.tomtom.dbf.speedrestrictions.SpeedRestriction.Validity.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SrDbfTest {
    @Test
    public void should_parse_sr() throws Exception {
        TomtomFolder folder = mock(TomtomFolder.class);
        when(folder.getFile("sr.dbf")).thenReturn(getClass().getResource("/tomtom/sr2.dbf").getPath());

        SrDbf srDbf = new SrDbf(folder);

        assertThat(srDbf.getSpeedRestrictions(12500067305696L)).containsExactly(
                new SpeedRestriction(12500067305696L, 30, positive),
                new SpeedRestriction(12500067305696L, 50, negative));

        assertThat(srDbf.getSpeedRestrictions(12500067332646L)).containsExactly(
                new SpeedRestriction(12500067332646L, 30, both));
    }
}