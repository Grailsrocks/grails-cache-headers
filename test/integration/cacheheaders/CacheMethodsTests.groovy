package cacheheaders

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import grails.test.*

class CacheMethodsTests extends GroovyTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    static RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz" // Always GMT

    String dateToHTTPDate(date) {

        def v = new SimpleDateFormat(RFC1123_DATE_FORMAT, Locale.ENGLISH)
        v.timeZone = TimeZone.getTimeZone('GMT')
        return v.format(date)
    }

    void testPresetCanTurnCachingOff() {
        def con = new com.grailsrocks.cacheheaders.TestController()
        
        ConfigurationHolder.config.cache.headers.presets.presetDeny = false
        
        con.presetTest1()
        
        assertEquals 'no-cache, no-store', con.response.getHeader('Cache-Control')
        assertNotNull con.response.getHeader('Expires')
        assertEquals 'no-cache', con.response.getHeader('Pragma')
    }

    void testValidUntil() {
        def con = new com.grailsrocks.cacheheaders.TestController()
        
        con.validUntilTest1()
        
        def d = con.request.getAttribute('test_validUntil')
        assertEquals d.time, con.response.getHeader('Expires') as Long
    }

    void testValidFor() {
        def con = new com.grailsrocks.cacheheaders.TestController()
        
        con.validForTest1()
        
        def cc = con.response.getHeader('Cache-Control').tokenize(',')*.trim()
        def ma = cc.find { it.startsWith('max-age=') }
        assertNotNull "Did not have max-age", ma
        assertEquals con.request.getAttribute('test_validFor'), (ma-'max-age=').toInteger()
    }
    
    void testValidForNegative() {
        def con = new com.grailsrocks.cacheheaders.TestController()
        
        con.validForTestNeg()
        
        def cc = con.response.getHeader('Cache-Control').tokenize(',')*.trim()
        def ma = cc.find { it.startsWith('max-age=') }
        assertNotNull "Did not have max-age", ma
        assertEquals 0, (ma-'max-age=').toInteger()
    }
    
    void testValidUntilNegative() {
        def con = new com.grailsrocks.cacheheaders.TestController()
        
        con.validUntilTestNeg()
        
        def cc = con.response.getHeader('Cache-Control').tokenize(',')*.trim()
        def ma = cc.find { it.startsWith('max-age=') }
        assertNotNull "Did not have max-age", ma
        assertEquals 0, (ma-'max-age=').toInteger()
    }
    
    void testCombinedStoreAndShareDefault() {
        // If we set store false, it should also default to share: private, specifying both
        def con = new com.grailsrocks.cacheheaders.TestController()
        
        con.combinedStoreAndShareDefaultTest()
        
        def ccParts = con.response.getHeader('Cache-Control').tokenize(',')*.trim()
        assertTrue "Did not contain 'private'", ccParts.contains('private')
        assertTrue "Did not contain 'no-store'", ccParts.contains('no-store')
    }
}
