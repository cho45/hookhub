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
		ret = [
			'var ret = [];',
			'ret.push(""'
		];

		var m, c, flag;
		while (( m = s.match(/<%(=*)/))) {
			flag = m[1];
			ret.push(',', uneval(s.slice(0, m.index)));
			s = s.slice(m.index + m[0].length);
			m = s.match(/(.*?)%>/);
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
				default:
					ret.push(");", c, "\nret.push(''");
					break;
			}
		}
		ret.push(
			',', uneval(s), ');',
			'return ret.join("");'
		);
		if (opts.useWith) {
			ret.unshift("with (s) {");
			ret.push("}");
		}
		ret.unshift(
			'var map = { "&" : "&amp;", "<" : "&lt;" , ">" : "&gt;"}, f = function (m) {return map[m]};',
			"return function (s) {"
		);
		ret.push("}");
		return new Function(ret.join(''));
	}
};

function stash (key) {
	return c.stash().apply(key)
}

user = c.user().getEmail();
author = stash("author");

new EJS(template).run({});

