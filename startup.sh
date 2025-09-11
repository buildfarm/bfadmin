#!/bin/bash

# Buildfarm Admin Startup Script
# This script builds and runs the Buildfarm Admin application

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN_DIR="${PROJECT_DIR}/main"
JAR_FILE="${MAIN_DIR}/target/bfadmin.jar"
DEFAULT_PORT=8080
JAVA_OPTS="${JAVA_OPTS:--Xmx1g -Xms512m}"

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Java is installed
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 11 or later."
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 11 ]; then
        print_warning "Java version is $JAVA_VERSION. Java 11 or later is recommended."
    else
        print_success "Java version $JAVA_VERSION detected"
    fi
}

# Function to check if Maven is installed
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven 3.6 or later."
        exit 1
    fi
    
    MVN_VERSION=$(mvn -version | head -n1 | cut -d' ' -f3)
    print_success "Maven version $MVN_VERSION detected"
}

# Function to check if port is available
check_port() {
    local port=$1
    if netstat -tuln 2>/dev/null | grep ":${port} " > /dev/null; then
        print_warning "Port $port is already in use. The application may fail to start."
        return 1
    fi
    return 0
}

# Function to build the application
build_application() {
    print_info "Building Buildfarm Admin application..."
    
    cd "$MAIN_DIR"
    
    # Clean and build
    print_info "Running Maven clean and package..."
    if mvn clean package -DskipTests; then
        print_success "Build completed successfully"
    else
        print_error "Build failed"
        exit 1
    fi
    
    # Check if JAR file exists
    if [ ! -f "$JAR_FILE" ]; then
        print_error "JAR file not found at $JAR_FILE"
        exit 1
    fi
    
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    print_success "JAR file created: $JAR_FILE (size: $JAR_SIZE)"
}

# Function to run the application
run_application() {
    print_info "Starting Buildfarm Admin application..."
    print_info "JAR file: $JAR_FILE"
    print_info "Java options: $JAVA_OPTS"
    print_info "Port: $DEFAULT_PORT"
    
    cd "$MAIN_DIR"
    
    print_info "Application will be available at: http://localhost:$DEFAULT_PORT"
    print_info "Press Ctrl+C to stop the application"
    print_info "----------------------------------------"
    
    # Run the application
    exec java $JAVA_OPTS -jar "$JAR_FILE"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -b, --build-only     Build the application but don't run it"
    echo "  -r, --run-only       Run the application without building (requires existing JAR)"
    echo "  -p, --port PORT      Set the server port (default: $DEFAULT_PORT)"
    echo "  -j, --java-opts OPTS Set Java JVM options (default: '-Xmx1g -Xms512m')"
    echo "  -h, --help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                   Build and run the application"
    echo "  $0 --build-only      Only build the application"
    echo "  $0 --run-only        Only run the existing application"
    echo "  $0 --port 9090       Run on port 9090"
    echo "  $0 --java-opts '-Xmx2g -Xms1g'  Run with custom JVM options"
}

# Parse command line arguments
BUILD_ONLY=false
RUN_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--build-only)
            BUILD_ONLY=true
            shift
            ;;
        -r|--run-only)
            RUN_ONLY=true
            shift
            ;;
        -p|--port)
            DEFAULT_PORT="$2"
            shift 2
            ;;
        -j|--java-opts)
            JAVA_OPTS="$2"
            shift 2
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate arguments
if [ "$BUILD_ONLY" = true ] && [ "$RUN_ONLY" = true ]; then
    print_error "Cannot specify both --build-only and --run-only"
    exit 1
fi

# Main execution
main() {
    print_info "Buildfarm Admin Startup Script"
    print_info "Project directory: $PROJECT_DIR"
    print_info "================================"
    
    # Check prerequisites
    check_java
    
    if [ "$RUN_ONLY" = false ]; then
        check_maven
    fi
    
    # Check port availability
    if [ "$RUN_ONLY" = true ] || [ "$BUILD_ONLY" = false ]; then
        check_port "$DEFAULT_PORT"
    fi
    
    # Build if needed
    if [ "$RUN_ONLY" = false ]; then
        build_application
    fi
    
    # Exit if build-only
    if [ "$BUILD_ONLY" = true ]; then
        print_success "Build completed. Use --run-only to start the application."
        exit 0
    fi
    
    # Check if JAR exists for run-only mode
    if [ "$RUN_ONLY" = true ] && [ ! -f "$JAR_FILE" ]; then
        print_error "JAR file not found at $JAR_FILE. Please build the application first."
        exit 1
    fi
    
    # Run the application
    run_application
}

# Trap signals for graceful shutdown
trap 'echo -e "\n${YELLOW}[INFO]${NC} Shutting down application..."; exit 0' INT TERM

# Execute main function
main "$@"
