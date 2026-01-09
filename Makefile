.PHONY: test

test:
	clj -M:test -m kaocha.runner --config-file test/tests.edn --skip-meta :integration

test-all:
	clj -M:test -m kaocha.runner  --config-file test/tests.edn
