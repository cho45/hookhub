Global = (function () {
	var _http = this._http;
	this.http = function (opts) {
		return _http.request(opts);
	}

	http.get   = function (url)       {
		return http({
			method : "get",
			url    : url
		})
	};

	http.post  = function (url, data) {
		return http({
			method  : "post",
			url     : url,
			data    : data,
			headers : {
				"Content-Type":"application/x-www-form-urlencoded"
			}
		})
	};

	delete this._http;
	delete stash.params.token;
	return this;
})();

