#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'custom_klaviyo_flutter'
  s.version          = '0.0.1'
  s.summary          = 'Klaviyo integration for Flutter'
  s.description      = <<-DESC
A new flutter plugin project.
                       DESC
  s.homepage         = 'https://github.com/saedkhaled/custom-klaviyo-flutter'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'saedkhaled96' => 'saedkhaled96@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'KlaviyoSwift', '~> 3.0.3'
  s.ios.deployment_target = '13.0'
  s.swift_version = '5.0'
end

