package com.grailsrocks.cacheheaders

class ResponseCacheHeadersFilters {
    CacheHeadersService cacheHeadersService

    def filters = {
        all(controller: '*', action: '*') {
            before = {
                if (ControllerAnnotationHelper.requiresAnnotation(NoCaching.class, controllerName, actionName)) {
                    cacheHeadersService.cache(response, false)
                }
            }
        }
    }

}
