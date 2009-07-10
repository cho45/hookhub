
Hookhub = {};

Hookhub.checkJavaScriptSyntax = function (str) {
	try {
		new Function(str);
	} catch (e) {

	}
};


$(function () {
	$("textarea[name=code]").each(function () {
		var $this = $(this);
		var error = $("<div class='error'>test</div>").hide();

		$this.parent().append(error);

		var id = setInterval(function () {
			try {
				new Function($this.val());
				error.hide();
				$this.removeClass("error");
			} catch (e) {
				error.show();
				error.text(String(e));
				$this.addClass("error");
			}
		}, 1000);
	});
	Hookhub.checkJavaScriptSyntax();
});

function log (a) {
	if (window.console) console.log(a);
}
