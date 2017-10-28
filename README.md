# AsyncRetryRequest

This async request is useful for scenarios where we need a response from one of several resources
in the shortest possible time but don't want to query more than one of them if we don't need to.
First one of the resources is queried, if a response is not received before a given timeout then
the second resource is queried as well. Once both resources have been queried we wait for the
first response from either of them.
