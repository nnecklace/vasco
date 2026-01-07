.PHONY: test

test:
	clj -M:test -m kaocha.runner --config-file test/tests.edn
