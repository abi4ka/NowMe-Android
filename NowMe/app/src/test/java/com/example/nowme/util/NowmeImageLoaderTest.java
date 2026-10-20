package com.example.nowme.util;

import com.bumptech.glide.load.model.GlideUrl;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NowmeImageLoaderTest {

    @BeforeClass
    public static void setUp() {
        if (System.getProperty("http.agent") == null) {
            System.setProperty("http.agent", "NowMe-Test");
        }
    }

    @Test
    public void getImageUrl_withNullId_returnsNull() {
        assertNull(NowmeImageLoader.getImageUrl(null));
    }

    @Test
    public void getImageUrl_withValidId_returnsCorrectUrl() {
        assertEquals("http://10.0.2.2:25565/nowme/123/image", NowmeImageLoader.getImageUrl(123L));
    }

    @Test
    public void buildGlideUrl_withNullId_returnsNull() {
        assertNull(NowmeImageLoader.buildGlideUrl(null));
    }

    @Test
    public void buildGlideUrl_withValidId_returnsNonNullInstance() {
        GlideUrl glideUrl = NowmeImageLoader.buildGlideUrl(123L);
        assertNotNull(glideUrl);
    }
}
