import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
    component: HomePage,
});

function HomePage() {
    return (
        <div className="flex min-h-svh flex-col items-center justify-center gap-10 p-2">
            <div className="flex flex-col items-center gap-4">
                <h1 className="font-bold text-3xl sm:text-4xl">React TanStarter</h1>
                <div className="flex items-center gap-2 text-foreground/80 text-sm max-sm:flex-col">
                    This is an unprotected page:
                    <pre className="rounded-md border bg-card p-1 text-card-foreground">routes/index.tsx</pre>
                </div>
            </div>

            <div className="flex flex-col items-center gap-2">
                <p className="text-foreground/80 max-sm:text-xs">
                    A minimal starter template for{" "}
                    <a
                        className="group text-foreground"
                        href="https://tanstack.com/start/latest"
                        rel="noreferrer noopener"
                        target="_blank"
                    >
                        🏝️ <span className="group-hover:underline">TanStack Start</span>
                    </a>
                    .
                </p>
                <div className="flex items-center gap-3">
                    <a
                        className="text-foreground/80 underline hover:text-foreground max-sm:text-sm"
                        href="https://github.com/dotnize/react-tanstarter"
                        rel="noreferrer noopener"
                        target="_blank"
                        title="Template repository on GitHub"
                    >
                        dotnize/react-tanstarter
                    </a>
                </div>
            </div>
        </div>
    );
}
