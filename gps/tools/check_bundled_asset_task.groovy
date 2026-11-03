// Execute the exact prepareBundledMapLibre body with local file/logger bindings.
// This checks asset validation and closure scope without assembling an APK.
def appDir = new File(args.length ? args[0] : 'app').canonicalFile
def source = new File(appDir, 'build.gradle').getText('UTF-8')
def startToken = '    doLast {'
int start = source.indexOf(startToken)
int end = source.indexOf('\n    }\n}\nafterEvaluate', start)
assert start >= 0 && end > start : 'Bundled asset task body not found'
def body = source.substring(start + startToken.length(), end)
def binding = new Binding()
binding.setVariable('file', { String path -> new File(appDir, path) })
binding.setVariable('logger', [lifecycle: { String message -> println message }])
def prefix = 'class GradleException extends RuntimeException { GradleException(String message) { super(message) } }\n'
new GroovyShell(binding).evaluate(prefix + body)
println 'Bundled Gradle task body OK: MapLibre, Brotli, licences, glyphs and shared digest closure'
