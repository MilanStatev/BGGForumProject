package com.example.bggforumproject.controllers.mvc;

import com.example.bggforumproject.models.ProfilePicture;
import com.example.bggforumproject.models.Reaction;
import com.example.bggforumproject.models.User;
import com.example.bggforumproject.models.enums.ReactionType;
import com.example.bggforumproject.security.CustomUserDetails;
import com.example.bggforumproject.service.contacts.PictureService;
import com.example.bggforumproject.service.contacts.ReactionService;
import com.example.bggforumproject.service.contacts.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/BGGForum/posts")
public class ReactionMvcController {

    private final ReactionService reactionService;
    private final UserService userService;
    private final PictureService pictureService;

    public ReactionMvcController(ReactionService reactionService, UserService userService, PictureService pictureService) {
        this.reactionService = reactionService;
        this.userService = userService;
        this.pictureService = pictureService;
    }

    @ModelAttribute("principalPhoto")
    public String principalPhoto(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        ProfilePicture profilePicture = pictureService.get(customUserDetails.getId());
        if (profilePicture != null) {
            return profilePicture.getPhotoUrl();
        }
        return "/images/blank_profile.png";
    }

    @ModelAttribute("isAuthenticated")
    public boolean populateIsAuthenticated(HttpSession session) {
        return session.getAttribute("currentUser") != null;
    }

    @ModelAttribute("requestURI")
    public String requestURI(final HttpServletRequest request) {
        return request.getRequestURI();
    }

    @GetMapping("/{postId}/reactions")
    public String getReactions(@PathVariable long postId){
        return null;
    }

    @PostMapping("/{postId}/reactions")
    public String createReaction(@PathVariable long postId,
                                 @ModelAttribute("reaction")ReactionType reactionType,
                                 HttpSession session){
        if(!populateIsAuthenticated(session)){
            return "redirect:/auth/login";
        }

        User user = userService.get((String) session.getAttribute("currentUser"));
        Reaction reaction = new Reaction();
        reaction.setReactionType(reactionType);

        reactionService.create(reaction, user, postId);

        return "redirect:/BGGForum/posts/{postId}";
    }

    @PostMapping("/{postId}/reactions/{reactionId}")
    public String updateReaction(@PathVariable long postId,
                                 @PathVariable long reactionId,
                                 @ModelAttribute("reaction") ReactionType reactionType,
                                 HttpSession session){
        if(!populateIsAuthenticated(session)){
            return "redirect:/auth/login";
        }

        User user = userService.get((String) session.getAttribute("currentUser"));
        reactionService.update(reactionId, user, postId, reactionType);

        return "redirect:/BGGForum/posts/{postId}";
    }

    @GetMapping("/{postId}/reactions/{reactionId}")
    public String deleteReaction(@PathVariable long postId,
                                 @PathVariable long reactionId,
                                 HttpSession session){

        if(!populateIsAuthenticated(session)){
            return "redirect:/auth/login";
        }

        User user = userService.get((String) session.getAttribute("currentUser"));

        reactionService.delete(reactionId, user, postId);
        return "redirect:/BGGForum/posts/{postId}";
    }
}
