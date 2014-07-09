module.exports = (grunt) ->
    mapName = (fileName) ->
        console.log fileName
        fileName + '.map'

    grunt.initConfig
        pkg: grunt.file.readJSON 'package.json'
        watch:
            files: ['public/notifications/coffee/*.coffee']
            tasks: ['coffee', 'uglify']
        coffee:
            compile:
                files: [
                    expand: true
                    cwd: 'public/notifications/coffee/'
                    src: ['*.coffee']
                    dest: 'public/notifications/js/'
                    ext: '.js'
                ]
                options:
                    sourceMap: true
        uglify:
            compress_target:
                files: [
                    expand: true,
                    cwd: 'public/notifications/js'
                    src: ['*.js', '!*.min.js']
                    dest: 'public/notifications/js'
                    ext: '.min.js'
                ]
                options:
                    sourceMapIncludeSources: true
                    sourceMapIn: mapName
                    sourceMap: true


    grunt.loadNpmTasks 'grunt-contrib-coffee'
    grunt.loadNpmTasks 'grunt-contrib-watch'
    grunt.loadNpmTasks 'grunt-contrib-uglify'
    grunt.registerTask 'default', ['watch']
    grunt.registerTask 'compile', ['coffee', 'uglify']
    return

