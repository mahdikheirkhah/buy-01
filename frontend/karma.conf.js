/* Karma configuration to ensure lcov coverage output for SonarQube */
module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
    ],
    client: {
      jasmine: {
        random: false,
      },
      clearContext: false, // keep Jasmine Spec Runner output visible in browser
    },
    reporters: ['progress', 'kjhtml', 'coverage'],
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/frontend'),
      reporters: [
        { type: 'html' },
        { type: 'lcovonly', subdir: '.' },
        { type: 'text-summary' },
      ],
      fixWebpackSourcePaths: true,
      check: {
        global: {
          statements: 0,
          branches: 0,
          functions: 0,
          lines: 0
        }
      },
      // Exclude files that are not directly testable or are configuration/routing
      exclude: [
        'src/main.ts',
        'src/app/app.config.ts',
        'src/app/app.routes.ts',
        'src/app/app.ts',
        'src/app/models/**/*.ts',
        'src/app/guards/**/*.ts',
        'src/app/interceptors/**/*.ts',
        'src/app/layouts/**/*.ts',
        'src/app/components/sidenav/**/*.ts',
        'src/app/components/navbar/**/*.ts',
        'src/app/components/confirm-dialog/**/*.ts',
        'src/app/components/password-confirm-dialog/**/*.ts',
        'src/app/components/image-cropper-modal/**/*.ts',
        'src/app/components/product-card/**/*.ts',
        'src/app/components/update-info-form/**/*.ts',
        'src/app/components/edit-product-modal/**/*.ts',
        '**/*.spec.ts'
      ]
    },
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: false,
    browsers: ['ChromeHeadless'],
    singleRun: true,
    restartOnFileChange: false,
  });
};