# Meal Tracking System - Modular Architecture

This directory contains the modular components for the Meal Tracking System.

## Component Structure

1. **Core Components:**
   - `MealEntryDialog.kt` - Main entry point for meal creation/editing (implemented)
   - `CategorySelectionScreen.kt` - First screen of meal entry flow for selecting meal category (implemented)
   - `FoodSearchScreen.kt` - Second screen of meal entry flow for searching and adding food items (implemented)
   - `ReviewScreen.kt` - Final screen of meal entry flow for reviewing and saving meal (implemented)
   - `MealLoggerCardNew.kt` - Enhanced card displaying meal summaries on the Health Screen (implemented)

2. **UI Components:**
   - `FoodSearchBar.kt` - Modern search component with suggestions, barcode and voice input buttons (implemented)
   - `FoodItemCard.kt` - Reusable card for displaying food items with expandable details (implemented)
   - `NutritionSummaryView.kt` - Visual summary of nutritional information with animated charts (implemented)
   - `MealCategorySelector.kt` - UI for selecting meal categories with icons and colors (implemented)
   - `NutrientBar.kt` - Animated progress bar for nutrient visualization (implemented)
   - `PortionSizeSelector.kt` - Visual portion size selection component

3. **Animation Components:**
   - `MealAnimations.kt` - Reusable animations for meal tracking components (implemented)
   - `RevealEffects.kt` - Staggered reveal animations for food items

4. **Functional Components:**
   - `BarcodeScanner.kt` - Barcode scanning using CameraX and ML Kit (implemented)
   - `VoiceInputHandler.kt` - Voice input processing using Android's SpeechRecognizer (implemented)
   - `NutritionCalculator.kt` - Utility for calculating nutritional values

5. **Data Components:**
   - `NutritionApiService.kt` - Interface for external nutrition API with mock implementation (implemented)
   - `LocalFoodDatabase.kt` - Room database for local caching of food data (implemented)
   - `FoodSearchRepository.kt` - Repository handling food search and meal operations (implemented)

6. **Integration Components:**
   - `MealEntryDialogIntegration.kt` - Bridges MealEntryDialog with NutritionViewModel (implemented)
   - `MealLoggerCardIntegration.kt` - Provides EnhancedMealLoggerCard for the Health Screen (implemented)
   - `AddMealFab.kt` - Floating action button for adding meals (implemented)

## Implementation Approach

1. Start with core components and ensure they work properly
2. Implement UI components with modern Material 3 design
3. Add animations and transitions for a polished experience
4. Integrate functional components for advanced features
5. Connect data components for real-world food information

## Current Status

- UI Components: All major UI components have been implemented with Material 3 design
- Integration: MealEntryDialog and MealLoggerCard are fully integrated with the Health Screen
- Animation: Core animations for smooth transitions and feedback are in place
- Functional Components: Barcode scanning and voice input are implemented
- Data Layer: Repository pattern implemented with mock data and Room database structure
- ViewModels: MealTrackingViewModel implemented to manage meal tracking state

## Next Steps

1. ✅ Integrate MealEntryDialog with NutritionViewModel (completed)
2. ✅ Replace old MealLoggerCard with EnhancedMealLoggerCard (completed)
3. Connect to real nutrition API (USDA FoodData Central or similar)
4. Implement custom food creation functionality
5. Add meal insights and analytics features
6. Thoroughly test all components for performance and edge cases
