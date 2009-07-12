EJS = function (template, opts) { return (this instanceof EJS) ? this.initialize(template, opts) : new EJS(template, opts) };
EJS.prototype = {
	initialize : function (template, opts) {
		this.template  = template;
		this.generator = this.compile(template, opts || {});

		this.processor = this.generator();
		// print(this.processor);
	},

	run : function (stash) {
		return this.processor(stash);
	},

	compile : function (s, opts) {
		s = String(s);
		var ret = [
			'var ret = [];',
			'ret.push(""'
		];

		var m, c, flag;
		while (( m = s.match(/<%((?:include)?(?:=*))/))) {
			flag = m[1];
			ret.push(',', uneval(s.slice(0, m.index)));
			s = s.slice(m.index + m[0].length);
			m = s.match(/([\s\S]*?)%>/);
			s = s.slice(m.index + m[0].length);
			c = m[1];
			switch (flag) {
				case "=":
				case "==":
					ret.push(', String(', c,').replace(/[&<>]/g, f)');
					break;
				case "===":
					ret.push(',', c);
					break;
				case "include=":
					var c = uneval(this.compile(opts.include(c), opts));
					ret.push(', ', c, '()(s)');
					break;
				default:
					ret.push(");", c, "\nret.push(''");
					break;
			}
		}
		ret.push(
			',', uneval(s), ");\n",
			'return ret.join("");'
		);
		if (opts.useWith) {
			ret.unshift("with (s) {");
			ret.push("}");
		}
		ret.unshift(
			'var map = { "&" : "&amp;", "<" : "&lt;" , ">" : "&gt;"}, f = function (m) {return map[m]};',
			"return function (s) {\n"
		);
		ret.push("\n}");

		return new Function(ret.join(''));
	}
};


Global = (function () {
	this.stash = function (key) {
		if (c.stash().contains(key)) {
			return c.stash().apply(key)
		}
		return null;
	}

	this.user   = String(stash("user"));
	this.author = String(stash("author"));

	this.cache = function (key, ifnone) {
		var ret = c.cache(key);
		if (ret === null) {
			ret = ifnone();
			c.cache(key, ret)
		}
		return ret;
	};

	return this;
})();


new EJS(template, {
	include : function (name) {
		name = name.replace(/^\s+|\s+$/g, "");
		return v.file(name);
	}
}).run({});

