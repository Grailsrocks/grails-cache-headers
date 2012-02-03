package com.grailsrocks.cacheheaders

class TestController {
    def presetTest1 = {
        cache "presetDeny"
    }

    def validUntilTest1 = {
        def d = new Date()+3
        request.test_validUntil = d
        
        cache validUntil: d
    }

    def validForTest1 = {
        request.test_validFor = 60
        
        cache validFor: 60
    }
    
    def validForTestNeg = {
        request.test_validFor = -60
        
        cache validFor: request.test_validFor
    }
    
    def validUntilTestNeg = {
        request.test_validUntil = new Date()-1
        
        cache validUntil: request.test_validUntil
    }
    
    def combinedStoreAndShareDefaultTest = {
        cache store: false
    }
}