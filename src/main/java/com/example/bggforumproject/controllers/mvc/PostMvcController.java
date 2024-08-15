package com.example.bggforumproject.controllers.mvc;

import com.example.bggforumproject.dtos.request.FilterDto;
import com.example.bggforumproject.dtos.response.CommentDTO;
import com.example.bggforumproject.dtos.response.PostCreateDTO;
import com.example.bggforumproject.dtos.response.PostUpdateDTO;
import com.example.bggforumproject.exceptions.AuthorizationException;
import com.example.bggforumproject.exceptions.EntityDuplicateException;
import com.example.bggforumproject.exceptions.EntityNotFoundException;
import com.example.bggforumproject.helpers.filters.CommentFilterOptions;
import com.example.bggforumproject.helpers.filters.PostFilterOptions;
import com.example.bggforumproject.helpers.filters.TagFilterOptions;
import com.example.bggforumproject.models.*;
import com.example.bggforumproject.service.contacts.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/BGGForum/posts")
public class PostMvcController {

    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    private final ReactionService reactionService;
    private final TagService tagService;
    private final ModelMapper mapper;

    @Autowired
    public PostMvcController(PostService postService, CommentService commentService,
                             UserService userService, ReactionService reactionService,
                             TagService tagService,
                             ModelMapper mapper) {
        this.postService = postService;
        this.commentService = commentService;
        this.userService = userService;
        this.reactionService = reactionService;
        this.tagService = tagService;
        this.mapper = mapper;
    }

    @ModelAttribute("isAdmin")
    public boolean populateIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.get(authentication.getName());

        List<User> admins = userService.getAllAdmins();
        return admins.contains(currentUser);
    }

    @ModelAttribute("isModerator")
    public boolean populateIsModerator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.get(authentication.getName());

        List<User> moderators = userService.getAllModerators();
        return moderators.contains(currentUser);
    }

    @ModelAttribute("requestURI")
    public String requestURI(final HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("comment")
    public CommentDTO populateCommentDto() {
        return new CommentDTO();
    }

    @ModelAttribute("tags")
    public List<Tag> populateTags() {
        return tagService.get(new TagFilterOptions(null, null, null, null, null));
    }

    //TODO: implement likes and dislikes counter
    /*
    @ModelAttribute("likes")
    public int populateLikesCount() {
        return reactionService.getLikesCount();
    }

    @ModelAttribute("dislikes")
    public List<Reaction> populateDislikes(){
        return reactionService.getDislikes;
    }*/

    @GetMapping
    public String getPosts(@RequestParam(value = "pageIndex", defaultValue = "0") int pageIndex,
                           @RequestParam(value = "pageSize", defaultValue = "5") int pageSize,
                           @ModelAttribute("postFilterOptions") FilterDto dto, Model model) {
        PostFilterOptions postFilterOptions = new PostFilterOptions(
                (dto.title() != null && dto.title().isEmpty()) ? null : dto.title(),
                (dto.content() != null && dto.content().isEmpty()) ? null : dto.content(),
                dto.userId(),
                (dto.tags() != null && dto.tags().isEmpty()) ? null : dto.tags(),
                (dto.postIds() != null && dto.postIds().isEmpty()) ? null : dto.postIds(),
                (dto.createCondition() != null && dto.createCondition().isEmpty()) ? null : dto.createCondition(),
                dto.created(),
                (dto.updateCondition() != null && dto.updateCondition().isEmpty()) ? null : dto.updateCondition(),
                dto.updated(),
                (dto.sortBy() != null && dto.sortBy().isEmpty()) ? null : dto.sortBy(),
                (dto.sortOrder() != null && dto.sortOrder().isEmpty()) ? null : dto.sortOrder()
        );

        Page<Post> posts = postService.get(postFilterOptions, pageIndex, pageSize);

        model.addAttribute("posts", posts.getContent());
        model.addAttribute("pagePosts", posts);
        model.addAttribute("currentPage", posts.getNumber() + 1);
        model.addAttribute("totalItems", posts.getTotalElements());
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("pageSize", pageSize);

        return "posts";
    }

    @GetMapping("/{postId}")
    public String getSinglePost(@RequestParam(value = "pageIndex", defaultValue = "0") int pageIndex,
                                @RequestParam(value = "pageSize", defaultValue = "5") int pageSize,
                                @PathVariable long postId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.get(authentication.getName());

//        PostOutFullDTO postOutFullDTO = mapper.map(postService.get(id), PostOutFullDTO.class);

        Post post = postService.get(postId);
        Page<Comment> all = commentService
                .getAll(new CommentFilterOptions(null, null, null, null, postId, null, null));

        Page<Comment> commentsForPost = commentService.getCommentsForPost(postId, pageIndex, pageSize);

        model.addAttribute("post", post);
        model.addAttribute("comments", commentsForPost);
        model.addAttribute("loggedUser", currentUser);
        return "post-single";
    }

    @GetMapping("/new")
    public String showNewPostPage(Model model) {
        model.addAttribute("post", new PostCreateDTO());
        return "create-post";
    }

    @PostMapping("/new")
    public String createPost(@Valid @ModelAttribute("post") PostCreateDTO dto,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails loggedUser) {
        User user = userService.get(loggedUser.getUsername());

        if (bindingResult.hasErrors()) {
            return "create-post";
        }
        Post post = mapper.map(dto, Post.class);
        postService.create(post, user);

        return "redirect:/BGGForum/posts";
    }

    @GetMapping("/{id}/update")
    public String showEditPostPage(@PathVariable long id, Model model) {

        try {
            Post post = postService.get(id);
            PostUpdateDTO dto = mapper.map(post, PostUpdateDTO.class);
            model.addAttribute("postId", id);
            model.addAttribute("post", dto);
            return "edit-post";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        }
    }

    @PostMapping("/{id}/update")
    public String updatePost(@PathVariable long id,
                             @Valid @ModelAttribute("post") PostUpdateDTO dto,
                             BindingResult bindingResult,
                             Model model,
                             @AuthenticationPrincipal UserDetails loggedUser) {

        User user = userService.get(loggedUser.getUsername());
        if (bindingResult.hasErrors()) {
            return "edit-post";
        }

        try {
            Post post = mapper.map(dto, Post.class);
            postService.update(id, post, user);
            return "redirect:/BGGForum/posts";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (EntityDuplicateException e) {
            bindingResult.rejectValue("title", "duplicate_post", e.getMessage());
            return "BeerUpdateView";
        } catch (AuthorizationException e) {
            model.addAttribute("statusCode", HttpStatus.UNAUTHORIZED.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        }
    }

    @GetMapping("/{postId}/delete")
    public String deletePost(@RequestParam(value = "pageIndex") int pageIndex,
                             @RequestParam(value = "pageSize") int pageSize,
                             @PathVariable long postId, Model model,
                             @AuthenticationPrincipal UserDetails loggedUser,
                             RedirectAttributes redirectAttributes) {

        User user = userService.get(loggedUser.getUsername());

        try {
            postService.delete(postId, user);
            redirectAttributes.addAttribute("pageIndex", pageIndex);
            redirectAttributes.addAttribute("pageSize", pageSize);
            return "redirect:/BGGForum/posts";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        } catch (AuthorizationException e) {
            model.addAttribute("statusCode", HttpStatus.UNAUTHORIZED.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "ErrorView";
        }
    }

}
