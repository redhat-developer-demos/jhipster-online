"""MCP server scaffold — extend with tools per https://github.com/modelcontextprotocol/python-sdk"""

from mcp.server.fastmcp import FastMCP

mcp = FastMCP("__PROJECT_NAME__")


@mcp.tool()
def __PYTHON_TOOL_FUNC__(message: str = "hello") -> str:
    """__MCP_TOOL_DESCRIPTION__"""
    return f"{message} from __PROJECT_NAME__"


def main() -> None:
    mcp.run()


if __name__ == "__main__":
    main()
