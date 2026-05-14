using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.Hosting;

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.UseUrls("http://0.0.0.0:__MCP_PORT__");
var app = builder.Build();
app.MapGet("/", () => "Wire ModelContextProtocol.AspNetCore here — see https://csharp.sdk.modelcontextprotocol.io/");
app.Run();
