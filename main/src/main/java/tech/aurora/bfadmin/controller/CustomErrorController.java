package tech.aurora.bfadmin.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom Error Controller for BuildFarm Admin
 * Handles application errors and displays custom error pages
 */
@Controller
public class CustomErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    @RequestMapping(ERROR_PATH)
    public String handleError(HttpServletRequest request, Model model) {
        // Get error attributes
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object error = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        // Set default values
        Integer statusCode = 500;
        String errorMessage = "Internal Server Error";
        String path = "/";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
            
            // Set user-friendly error messages based on status code
            switch (statusCode) {
                case 404:
                    errorMessage = "Page Not Found";
                    break;
                case 403:
                    errorMessage = "Access Forbidden";
                    break;
                case 401:
                    errorMessage = "Unauthorized Access";
                    break;
                case 500:
                    errorMessage = "Internal Server Error";
                    break;
                default:
                    errorMessage = HttpStatus.valueOf(statusCode).getReasonPhrase();
            }
        }

        if (requestUri != null) {
            path = requestUri.toString();
        }

        // Add attributes to model
        model.addAttribute("status", statusCode);
        model.addAttribute("error", errorMessage);
        model.addAttribute("path", path);
        model.addAttribute("timestamp", timestamp);
        model.addAttribute("currentPage", "error");

        // Add detailed message for specific errors
        if (error != null) {
            model.addAttribute("message", error.toString());
        } else {
            // Add contextual help message based on status code
            String helpMessage = getHelpMessage(statusCode);
            if (helpMessage != null) {
                model.addAttribute("message", helpMessage);
            }
        }

        // Add stack trace for development (only if exception exists and we're in debug mode)
        if (exception != null && exception instanceof Exception) {
            Exception ex = (Exception) exception;
            // Only show stack trace in development mode
            // You can add a check for profile here if needed
            StringBuilder trace = new StringBuilder();
            trace.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append("\n");
            for (StackTraceElement element : ex.getStackTrace()) {
                trace.append("\tat ").append(element.toString()).append("\n");
            }
            model.addAttribute("trace", trace.toString());
        }

        return "error";
    }

    private String getHelpMessage(int statusCode) {
        switch (statusCode) {
            case 404:
                return "The page you are looking for might have been moved, deleted, or you entered the wrong URL.";
            case 403:
                return "You don't have permission to access this resource. Please check with your administrator.";
            case 401:
                return "Authentication is required to access this resource. Please log in and try again.";
            case 500:
                return "A server error occurred while processing your request. Please try again later.";
            case 503:
                return "The service is temporarily unavailable. Please try again in a few minutes.";
            default:
                return null;
        }
    }

    public String getErrorPath() {
        return ERROR_PATH;
    }
}
