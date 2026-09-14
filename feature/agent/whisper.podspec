Pod::Spec.new do |s|
  s.name             = 'whisper'
  s.version          = '1.9.1'
  s.summary          = 'Vendored whisper.cpp xcframework for on-device Whisper speech-to-text (iOS).'
  s.homepage         = 'https://github.com/ggml-org/whisper.cpp'
  s.license          = { :type => 'MIT' }
  s.author           = { 'ggml-org' => 'https://github.com/ggml-org' }
  s.source           = { :path => '.' }
  s.ios.deployment_target = '16.0'

  # feature/agent's cinterop only binds whisper.h (headers) for Kotlin compilation — the actual
  # binary must be linked into the final iOS app separately. This vendors the same xcframework
  # fetched by the :feature:agent Gradle `downloadWhisperXcframework` task (build.gradle.kts), so
  # `pod install` embeds+links it into iosApp. Not committed to git: run a Gradle build for
  # :feature:agent at least once (downloads the zip) before `pod install`.
  s.vendored_frameworks = 'build/whisper-xcframework/extracted/build-apple/whisper.xcframework'
end
