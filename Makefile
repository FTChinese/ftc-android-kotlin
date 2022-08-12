version := `git describe --tags`

.PHONY: version
version :
	@echo $(version)

.PHONY: upload
upload :
	mc cp ./app/build/outputs/ftc/release/ftchinese-$(version)-ftc-release.apk ftc/android
