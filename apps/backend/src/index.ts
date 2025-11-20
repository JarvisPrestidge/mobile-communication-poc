import { Hono } from "hono";
import { createBunWebSocket } from "hono/bun";
import { cors } from "hono/cors";
import type { WSContext } from "hono/ws";

const { upgradeWebSocket, websocket } = createBunWebSocket();

const app = new Hono();

// Store connected WebSocket clients
const connectedClients = new Set<WSContext<unknown>>();

// Enable CORS for the web app
app.use(
    "*",
    cors({
        origin: ["http://localhost:3000", "http://10.0.2.2:3000"],
        credentials: true,
    })
);

// Health check endpoint
app.get("/", (c) => c.json({ status: "ok", message: "Hono backend ready" }));

app.get("/api/health", (c) =>
    c.json({
        status: "healthy",
        timestamp: new Date().toISOString(),
        connectedClients: connectedClients.size,
    })
);

// WebSocket endpoint for native app connections
app.get(
    "/ws",
    upgradeWebSocket((_c) => {
        console.log("WebSocket upgrade request received");

        return {
            onOpen(_event, ws) {
                console.log("WebSocket connection opened");
                connectedClients.add(ws);
                console.log(`Connected clients: ${connectedClients.size}`);

                // Send welcome message to the newly connected client
                ws.send(
                    JSON.stringify({
                        type: "connection_established",
                        data: {
                            message: "Connected to backend WebSocket",
                            timestamp: new Date().toISOString(),
                        },
                    })
                );
            },

            onMessage(event, ws) {
                console.log(`Message from client: ${event.data}`);

                try {
                    const message = JSON.parse(event.data.toString());

                    // Handle different message types from native client
                    if (message.type === "ping") {
                        ws.send(
                            JSON.stringify({
                                type: "pong",
                                data: { timestamp: new Date().toISOString() },
                            })
                        );
                    } else if (message.type === "register") {
                        console.log("Native app registered:", message.data);
                        ws.send(
                            JSON.stringify({
                                type: "registration_success",
                                data: { clientId: message.data?.deviceId || "unknown" },
                            })
                        );
                    }
                } catch (error) {
                    console.error("Error parsing WebSocket message:", error);
                }
            },

            onClose(_event, ws) {
                console.log("WebSocket connection closed");
                connectedClients.delete(ws);
                console.log(`Connected clients: ${connectedClients.size}`);
            },

            onError(event) {
                console.error("WebSocket error:", event);
            },
        };
    })
);

// HTTP endpoint for Pattern C: Web app sends card design, backend notifies native app
app.post("/api/card/design", async (c) => {
    try {
        const body = await c.req.json();
        console.log("Card design received:", body);

        // Validate the request
        if (!body.cardData) {
            return c.json({ error: "Missing cardData" }, 400);
        }

        // Process the card design (mock processing)
        const processedDesign = {
            id: `design-${Date.now()}`,
            ...body.cardData,
            processedAt: new Date().toISOString(),
            status: "processed",
        };

        // Notify all connected native clients via WebSocket
        const notification = {
            type: "design_processed",
            data: processedDesign,
        };

        const notificationMessage = JSON.stringify(notification);
        let notifiedCount = 0;

        for (const client of connectedClients) {
            try {
                client.send(notificationMessage);
                notifiedCount += 1;
            } catch (error) {
                console.error("Error sending to client:", error);
                connectedClients.delete(client);
            }
        }

        console.log(`Notified ${notifiedCount} connected clients`);

        // Return success response to web app
        return c.json({
            success: true,
            design: processedDesign,
            notifiedClients: notifiedCount,
        });
    } catch (error) {
        console.error("Error processing card design:", error);
        return c.json({ error: "Internal server error" }, 500);
    }
});

export default {
    port: 3001,
    fetch: app.fetch,
    websocket,
};
