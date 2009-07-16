// Example:
//
stash.config.foobar;
stash.params.foobar;

http({
    method: "post",
    url: "http://example.com/",
    data: http.data({ foo: bar }),
    headers : {
        Authorization : "Basic " + util.base64.encode(
            [stash.config.xxx_user, stash.config.xxx_pass].join(":")
        )
    }
}).code

