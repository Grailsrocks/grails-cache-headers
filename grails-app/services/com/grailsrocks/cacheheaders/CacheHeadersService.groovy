package com.grailsrocks.cacheheaders

import java.text.SimpleDateFormat

class CacheHeadersService {

    static transactional = false

    boolean enabled = true
    
    def presets
    
    void lastModified(response, dateOrLong) {
        response.setDateHeader('Last-Modified', dateOrLong instanceof Date ? dateOrLong.time : dateOrLong.toLong() )
    }
    
    void cache(response, Boolean allow) {
        if (enabled) {
            if (!allow) {
                response.setHeader('Cache-Control', 'no-cache, no-store')
                response.setDateHeader('Expires', (new Date()-1).time )
                response.setHeader('Pragma', 'no-cache')
            } else {
                throw new IllegalArgumentException("Call to [cache] with [true] doesn't make sense, you can only pass false. Pass in a map with settings to control caching.")
            }
        }
    }
    
    void cache(response, String presetName) {
        if (enabled) {
            def p = presets[presetName]
            if ((p == null) || (p instanceof ConfigObject && p.empty)) {
                throw new IllegalArgumentException("Call to [cache] with preset name [$presetName] but no such preset found in config")
            }
            // Witness the power of groovy. p might be boolean or Map. Selects correct method.
            cache(response, p)
        }
    }
    
    void cache(response, Map args) {
        if (!enabled) {
            return
        }
        
        def store = args.store
        def share = args.shared
        def validFor = args.validFor
        def validUntil = args.validUntil
        def neverExpires = args.neverExpires
        def requiresAuth = args.auth

        def now = new Date()

        // Get our info together
        def expiresOn 
        def maxage
        if (validFor != null) {
            expiresOn = new Date(now.time + validFor*1000L)
            maxage = Math.max(0, validFor)
        } else if (validUntil != null) {
            expiresOn = validUntil
            maxage = Math.round( Math.max(0, validUntil.time-now.time) / 1000L)
        } else if (neverExpires) {
            // HTTP 1.1 spec says SHOULD NOT set more than 1 yr in future
            // @todo Investigate if impls of servletresponse.setDateHeader() are using efficient threadlocals,
            // and if so change to use those
            expiresOn = now + 365
            maxage = Math.round( Math.max(0, expiresOn.time-now.time) / 1000L)
        }
        
        def cacheControl = []
        
        // Now set the headers
        if ((store != null) && !store) {
            cacheControl << 'no-store'
        }
        
        // Always set private if no explicit share - help grails devs by defaulting to safest
        if (share) {
            cacheControl << 'public'
            // Note, for authentication sites we still need to add no-cache to force verification
            // to which the app can return "not modified" if it handles etag/lastmod
            if (requiresAuth) {
                cacheControl <<  'no-cache'
            }
        } else {
            cacheControl << 'private'
        }
        
        if (maxage != null) {
            if (share) {
                cacheControl << "s-maxage=$maxage"
            }
            // Always set max-age anyway, even if shared. Browsers may not pick up on s-maxage
            cacheControl << "max-age=$maxage"
        }

        if (cacheControl) {
            response.setHeader('Cache-Control', cacheControl.join(', '))
        }
        
        if (expiresOn != null) {
            response.setDateHeader('Expires', expiresOn.time)
        }

        // Always set last modified for courtesy and older clients, 
        // only if not already set by application (load balancers need identical lastmods)
        if (!response.containsHeader('Last-Modified')) {
            lastModified(response, now)
        }
    }
     
    boolean withCacheHeaders(context, Closure dsl) {
        assert dsl != null
        
        // The lightest DSL impl in the world..?
        def etagDSL
        def lastModDSL
        def generateDSL
        dsl.delegate = new Expando(
            etag: { c -> etagDSL = c },
            lastModified: { c -> 
                lastModDSL = c 
            },
            generate: { c -> generateDSL = c }
        )
        dsl.resolveStrategy = Closure.DELEGATE_FIRST
        dsl.call()

        if (!enabled) {
            callClosure(generateDSL, context)

            return true
        }
        
        def request = context.request
        def response = context.response
        
        def possibleTags = request.getHeader('If-None-Match')
        def modifiedDate = -1
        try {
            modifiedDate = request.getDateHeader('If-Modified-Since')
        } catch (IllegalArgumentException iae) {
            // nom nom nom
            log.error "Couldn't parse If-Modified-Since header", iae
        }
        def etag
        def lastMod
        
        def etagChanged = false
        def lastModChanged = false
        
        // If we have some headers, let's run the appropriate DSLs
        if (possibleTags || (modifiedDate != -1)) {

            // First let's check for ETags, they are 1st class
            if (possibleTags && etagDSL) {
                def tagList = possibleTags.split(',')*.trim()
                etag = callClosure(etagDSL, context)
                if (log.debugEnabled) {
                    log.debug "There was a list of ETag candidates supplied [${tagList}], calculated new ETag... ${etag}"
                }
                if (!tagList.contains(etag)) {
                    etagChanged = true
                }
            } 
            
            if ((modifiedDate != -1) && lastModDSL) {
                // Or... 2nd class... check lastmod
                def compareDate = new Date(modifiedDate)
                lastMod = callClosure(lastModDSL, context)

                if (compareDate != lastMod) {
                    lastModChanged = true
                }
            }

            // If neither has changed, we 304. But if either one has changed, we don't
            if (!lastModChanged && !etagChanged) {
                response.sendError(304) // Not modified
                return false
            }
        }
        
        // If we get here, no headers or it has changed
        
        if (!etag && etagDSL) {
            etag = callClosure(etagDSL, context)
        }
        if (!lastMod && lastModDSL) {
            lastMod = callClosure(lastModDSL, context)
        }
        
        if (etag) {
            response.setHeader('ETag', etag)
        }
        if (lastMod) {
            lastModified(response, lastMod)
        }
        
        callClosure(generateDSL, context)

        return true
    }
    
    def callClosure(dsl, delegate) {
        dsl.delegate = delegate
        dsl.resolveStrategy = Closure.DELEGATE_FIRST
        dsl()
    }

}
