# If you have trouble with an update, run the build with --no-cache

if [ `uname -p` = 'arm' ]; then
    ARCH=aarch64
else
    ARCH=x86_64
fi

docker build . --platform=linux/$ARCH --file Dockerfile --tag xmlcalabash-sendria
