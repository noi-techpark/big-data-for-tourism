#!/bin/bash

mkdir -p ~/.m2
cat > ~/.m2/settings.xml << EOF
<settings>
    <localRepository>$PWD/docker/.m2</localRepository>
</settings>
EOF

export MAVEN_CONFIG="$HOME"

/bin/bash -c "/usr/local/bin/mvn-entrypoint.sh $@"
