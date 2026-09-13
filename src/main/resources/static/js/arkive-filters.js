(function () {
	"use strict";

	var DEBOUNCE_MS = 350;
	var FOCUS_STORAGE_KEY = "arkive:autoFilterFocus";

	function saveFocusState(form) {
		var field = document.activeElement;

		if (!field || !form.contains(field) || !field.matches("input, select, textarea")) {
			return;
		}

		var focusState = {
			path: window.location.pathname,
			formAction: form.action,
			id: field.id || "",
			name: field.name || "",
			selectionStart: null,
			selectionEnd: null
		};

		try {
			if (typeof field.selectionStart === "number" && typeof field.selectionEnd === "number") {
				focusState.selectionStart = field.selectionStart;
				focusState.selectionEnd = field.selectionEnd;
			}

			window.sessionStorage.setItem(FOCUS_STORAGE_KEY, JSON.stringify(focusState));
		} catch (error) {
			// Ignore unavailable storage or unsupported selection APIs.
		}
	}

	function submitForm(form) {
		saveFocusState(form);

		if (typeof form.requestSubmit === "function") {
			form.requestSubmit();
			return;
		}
		form.submit();
	}

	function findRestoredField(form, focusState) {
		var field = null;

		if (focusState.id) {
			field = document.getElementById(focusState.id);
		}

		if (!field && focusState.name && form.elements[focusState.name]) {
			field = form.elements[focusState.name];
		}

		if (field && form.contains(field) && field.matches("input, select, textarea")) {
			return field;
		}

		return null;
	}

	function restoreFocusState() {
		var rawFocusState;
		var focusState;

		try {
			rawFocusState = window.sessionStorage.getItem(FOCUS_STORAGE_KEY);
			window.sessionStorage.removeItem(FOCUS_STORAGE_KEY);
		} catch (error) {
			return;
		}

		if (!rawFocusState) {
			return;
		}

		try {
			focusState = JSON.parse(rawFocusState);
		} catch (error) {
			return;
		}

		if (focusState.path !== window.location.pathname) {
			return;
		}

		var restored = false;

		document.querySelectorAll("form[data-auto-filter]").forEach(function (form) {
			var field;

			if (restored || form.action !== focusState.formAction) {
				return;
			}

			field = findRestoredField(form, focusState);

			if (!field) {
				return;
			}

			restored = true;

			window.requestAnimationFrame(function () {
				try {
					field.focus({ preventScroll: true });
				} catch (error) {
					field.focus();
				}

				if (
					typeof field.setSelectionRange === "function" &&
					typeof focusState.selectionStart === "number" &&
					typeof focusState.selectionEnd === "number"
				) {
					try {
						field.setSelectionRange(focusState.selectionStart, focusState.selectionEnd);
					} catch (error) {
						// Some input types do not support manual selection.
					}
				}
			});
		});
	}

	function bindAutoFilter(form) {
		var debounceTimer = null;
		var searchFields = form.querySelectorAll("[data-filter-search]");
		var instantFields = form.querySelectorAll("select, input[type='checkbox'], input[type='radio']");

		searchFields.forEach(function (field) {
			field.addEventListener("input", function () {
				window.clearTimeout(debounceTimer);
				debounceTimer = window.setTimeout(function () {
					submitForm(form);
				}, DEBOUNCE_MS);
			});
		});

		instantFields.forEach(function (field) {
			field.addEventListener("change", function () {
				window.clearTimeout(debounceTimer);
				submitForm(form);
			});
		});
	}

	document.addEventListener("DOMContentLoaded", function () {
		document.querySelectorAll("form[data-auto-filter]").forEach(bindAutoFilter);
		restoreFocusState();
	});
})();
