# Chinese Chess Online Server

Container-friendly online room service for the Android app. It uses the same HTTP API as the app client and can run on Render, Fly.io, Railway, or any platform that exposes a web container.

## Run Locally

```sh
npm start
```

The server listens on `PORT`, defaulting to `10000`.

## Deploy On Render

1. Create a new Web Service.
2. Point Render at `ChineseChessAndroid/online-server`.
3. Use the included `../render.yaml`, Docker deployment, or use:
   - Build command: empty
   - Start command: `npm start`
4. Set the Android app URL in `ChineseChessAndroid/gradle.properties`:

```properties
onlineServerUrl=https://your-render-service.onrender.com
```

Rooms are kept in process memory. For one Render instance this is enough for lightweight play, but rooms reset when the service restarts.

## API

- `POST /api/rooms/{roomId}/join`
- `GET /api/rooms/{roomId}?playerId={playerId}`
- `POST /api/rooms/{roomId}/move`
- `POST /api/rooms/{roomId}/action`
